package editor.data;

import editor.lang.NullArgumentException;
import editor.util.Files;
import editor.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class CsvImporter implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(CsvImporter.class);

	private final char separator;

	public CsvImporter(boolean useTab) {
		separator = useTab ? '\t' : ',';
	}

	public CsvImporter() {
		this(false);
	}

	public boolean importFile(OptionFile of, File source) {
		if (null == of) {
			throw new NullArgumentException("of");
		}
		if (null == source) {
			throw new NullArgumentException("source");
		}
		log.debug("Try to import CSV file: {}", source);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(source), "UTF-8"));
			String line = reader.readLine();

			// Parse header
			String[] header = null;
			int idCol = -1;
			if (line != null && isHeader(line)) {
				header = line.split(Pattern.quote(String.valueOf(separator)));
				for (int i = 0; i < header.length; i++) {
					if (header[i].replace("\"", "").trim().equalsIgnoreCase("ID")) {
						idCol = i;
						break;
					}
				}
				line = reader.readLine();
			}

			while (line != null) {
				if (line.trim().length() > 0) {
					String[] values = line.split(Pattern.quote(String.valueOf(separator)));
					int playerId = -1;
					if (idCol >= 0 && values.length > idCol && !Strings.isEmpty(values[idCol])) {
						try {
							playerId = Integer.parseInt(values[idCol].replace("\"", "").trim());
							log.debug("Parsed playerId: {}, FIRST_CLUB: {}, FIRST_CLUB+TOTAL: {}",
									playerId, Player.FIRST_CLUB, Player.FIRST_CLUB + Player.TOTAL_CLUB_PLAYERS);
						} catch (NumberFormatException e) {
							log.warn("Invalid ID value '{}', skipping line: {}", values[idCol], line);
							line = reader.readLine();
							continue;
						}
					}

					int playerIndex = -1;
					if (playerId >= Player.FIRST_EDIT && playerId < Player.END_EDIT) {
						// Edited player range — direct slot
						playerIndex = playerId;
					} else if (playerId >= 1 && playerId < Player.TOTAL) {
						// Club/classic/national range — treat ID as direct slot index
						playerIndex = playerId;
					} else {
						log.warn("ID {} out of valid range, skipping line: {}", playerId, line);
						line = reader.readLine();
						continue;
					}

					if (playerIndex < 0) {
						for (int i = Player.FIRST_CLUB; i < Player.FIRST_CLUB + Player.TOTAL_CLUB_PLAYERS; i++) {
							if (new Player(of, i).isEmpty()) {
								playerIndex = i;
								break;
							}
						}
					}

					if (playerIndex < 0) {
						log.warn("No matching player slot found for ID {}, skipping line: {}", playerId, line);
						line = reader.readLine();
						continue;
					}

					boolean ok = importPlayer(line, of, playerIndex, idCol, playerId);
					if (!ok) {
						log.warn("Failed to parse line for player #{}: {}", playerIndex, line);
					}
				}
				line = reader.readLine();
			}

			log.debug("Importing of CSV file '{}' succeeded", source);
			return true;
		} catch (IOException e) {
			log.error("Failed to import CSV file:", e);
			return false;
		} finally {
			Files.closeStream(reader);
		}
	}

	private boolean isHeader(String line) {
		String upper = line.toUpperCase();
		return upper.contains("NAME") || upper.contains("AGE") || upper.contains("HEIGHT");
	}

	private boolean importPlayer(String line, OptionFile of, int player, int idCol, int playerId) {
		String[] values = line.split(Pattern.quote(String.valueOf(separator)));
		int col = 0;
		if (idCol >= 0) col++;
		try {
			Stats.setValue(of, player, Stats.PLAYER_ID, playerId);
			// Name
			String newName = values[col++].replace("\"", "").trim();
			if (!Strings.isEmpty(newName)) {
				new Player(of, player).setName(newName);
			}
			// Age
			Stats.setValue(of, player, Stats.AGE, parseInt(values[col++]) - 15);
			// Height
			Stats.setValue(of, player, Stats.HEIGHT, parseInt(values[col++]) - 148);
			// Weight
			Stats.setValue(of, player, Stats.WEIGHT, parseInt(values[col++]));
			// Foot
			String foot = values[col++];
			Stats.setValue(of, player, Stats.FOOT, foot.contains("R") ? 0 : 1);

			// Favoured Side
			col++;

			// Roles
			for (Stat s : Stats.ROLES) {
				String val = values[col++];
				if (!Strings.isEmpty(val.trim())) {
					Stats.setValue(of, player, s, parseInt(val));
				}
			}

			// Registered Position
			String regPosStr = values[col++].replace("\"", "").trim();
			if (!Strings.isEmpty(regPosStr)) {
				int regPosVal = -1;
				try {
					regPosVal = Integer.parseInt(regPosStr);
				} catch (NumberFormatException e) {
					String[] posNames = {"GK", "SW", "CB", "SB", "DMF", "WB", "CMF", "SMF", "AMF", "WF", "SS", "CF"};
					for (int p = 0; p < posNames.length; p++) {
						if (posNames[p].equalsIgnoreCase(regPosStr)) {
							regPosVal = p;
							break;
						}
					}
				}
				if (regPosVal >= 0) {
					Stats.setValue(of, player, Stats.REG_POS, regPosVal);
				} else {
					log.warn("Unknown registered position '{}' for player {}", regPosStr, player);
				}
			}

			// Other Stats
			setIfNotBlank(of, player, Stats.INJURY, values[col++], true);
			setIfNotBlank(of, player, Stats.DRIBBLE_STYLE, values[col++], false);
			setIfNotBlank(of, player, Stats.FREE_KICK, values[col++], false);
			setIfNotBlank(of, player, Stats.PK_STYLE, values[col++], false);
			setIfNotBlank(of, player, Stats.DK_STYLE, values[col++], false);

			setIfNotBlank(of, player, Stats.GROWTH, values[col++], false);
			setIfNotBlank(of, player, Stats.FACE, values[col++], false);
			setIfNotBlank(of, player, Stats.FACE_TYPE, values[col++], false);
			setIfNotBlank(of, player, Stats.SKIN, values[col++], false);
			setIfNotBlank(of, player, Stats.HEAD_LENGTH, values[col++], false);
			setIfNotBlank(of, player, Stats.HEAD_WIDTH, values[col++], false);
			setIfNotBlank(of, player, Stats.HAIR, values[col++], false);

			for (Stat s : Stats.ABILITY99) {
				String val = values[col++];
				if (!Strings.isEmpty(val.trim())) {
					Stats.setValue(of, player, s, parseInt(val));
				}
			}

			setIfNotBlank(of, player, Stats.CONDITION, values[col++], false);
			setIfNotBlank(of, player, Stats.WEAK_FOOT_ACC, values[col++], false);
			setIfNotBlank(of, player, Stats.WEAK_FOOT_FREQ, values[col++], false);
			setIfNotBlank(of, player, Stats.CONSISTENCY, values[col++], false);

			for (Stat s : Stats.ABILITY_SPECIAL) {
				String val = values[col++];
				if (!Strings.isEmpty(val.trim())) {
					Stats.setValue(of, player, s, parseInt(val));
				}
			}

// Nationality
			String natStr = values[col++].replace("\"", "").trim();
			if (!Strings.isEmpty(natStr)) {
				int natVal = -1;
				// Try parsing as number first
				try {
					natVal = Integer.parseInt(natStr);
				} catch (NumberFormatException e) {
					// Look up by name in NATIONAL_TEAMS
					for (int n = 0; n < Squads.NATIONAL_TEAMS.length; n++) {
						if (Squads.NATIONAL_TEAMS[n].equalsIgnoreCase(natStr)) {
							natVal = n;
							break;
						}
					}
				}
				if (natVal >= 0) {
					Stats.setValue(of, player, Stats.NATIONALITY, natVal);
				} else {
					log.warn("Unknown nationality '{}' for player {}", natStr, player);
				}
			}
// Skip remaining columns (international number, classic number, club team, club number)Now Bull

			return true;
		} catch (NumberFormatException e) {
			log.warn("Invalid number for player {} at column {}", player, col, e);
			return false;
		} catch (Exception e) {
			log.warn("General error for player {}: {}", player, e.getMessage());
			return false;
		}
	}

	private int parseInt(String val) {
		if (Strings.isEmpty(val)) return 0;
		val = val.replace("\"", "").trim();
		if (Strings.isEmpty(val)) return 0;
		return Integer.parseInt(val);
	}

	private void setIfNotBlank(OptionFile of, int player, Stat stat, String val, boolean isChar) {
		val = val.replace("\"", "").trim();
		if (!Strings.isEmpty(val)) {
			Stats.setValue(of, player, stat, isChar ? parseChar(val) : Integer.parseInt(val));
		}
	}

	private int parseChar(String val) {
		val = val.replace("\"", "").trim();
		if (Strings.isEmpty(val)) return 0;
		char c = val.charAt(0);
		if (c >= 'A' && c <= 'C') {
			return c - 'A';
		}
		return 0;
	}
}
