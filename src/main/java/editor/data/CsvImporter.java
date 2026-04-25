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
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8));
			String line = reader.readLine();

			// Skip header if it is present
			if (line != null && isHeader(line)) {
				line = reader.readLine();
			}

			// We will assume that players match lines sequentially from player 1 up to MAX,
			// or we can adjust logic if player index is encoded.
			// Current CsvMaker exports in order without an ID column.
			int playerId = 1;
			while (line != null && playerId < Player.TOTAL_EDIT + Player.FIRST_EDIT) {
				if (playerId == Player.FIRST_UNUSED) {
					// According to CsvMaker, we might skip extra unused players
					playerId = Player.FIRST_EDIT;
				}

				if (!line.trim().isEmpty()) {
					boolean ok = importPlayer(line, of, playerId);
					if (ok) {
						playerId++;
					} else {
						log.warn("Failed to parse line for player #{}: {}", playerId, line);
					}
				}
				line = reader.readLine();
			}

			log.debug("Importing of CSV file '{}' succeeded", source);
			return true;
		} catch (IOException e) {
			log.error("Failed to import CSV file:", e);
		} finally {
			Files.closeStream(reader);
		}

		return false;
	}

	private boolean isHeader(String line) {
		// Just a heuristic: if first column has letters, it is probably the header
		// Or we can check if it starts with "Name"
		return line.startsWith("Name") || line.contains("Age") || line.contains("Height");
	}

	private boolean importPlayer(String line, OptionFile of, int player) {
		String[] values = line.split(Pattern.quote(String.valueOf(separator)));
		if (values.length < 50) { // Just a sanity check
			return false;
		}

		int col = 0;
		try {
			// Name
			String name = values[col++].trim();
			// We can implement Name setting later, we need to know the string encoding methods

			// Age
			Stats.setValue(of, player, Stats.AGE, parseInt(values[col++]));
			// Height
			Stats.setValue(of, player, Stats.HEIGHT, parseInt(values[col++]));
			// Weight
			Stats.setValue(of, player, Stats.WEIGHT, parseInt(values[col++]));
			// Foot
			String foot = values[col++];
			Stats.setValue(of, player, Stats.FOOT, foot.contains("R") ? 0 : 1);

			// Favorite Side
			String side = values[col++];
			int sideVal = 0; // default Same Side
			// Parse side string if needed...

			// Roles
			for (Stat s : Stats.ROLES) {
				Stats.setValue(of, player, s, parseInt(values[col++]));
			}

			// Registered Position
			String regPosStr = values[col++];
			// Find the index in ROLES
			// Stats.setValue(of, player, Stats.REG_POS, posIndex);

			// Other Stats
			Stats.setValue(of, player, Stats.INJURY, parseChar(values[col++]));
			Stats.setValue(of, player, Stats.DRIBBLE_STYLE, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.FREE_KICK, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.PK_STYLE, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.DK_STYLE, parseInt(values[col++]));

			Stats.setValue(of, player, Stats.GROWTH, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.FACE, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.FACE_TYPE, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.SKIN, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.HEAD_LENGTH, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.HEAD_WIDTH, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.HAIR, parseInt(values[col++]));

			for (Stat s : Stats.ABILITY99) {
				Stats.setValue(of, player, s, parseInt(values[col++]));
			}

			Stats.setValue(of, player, Stats.CONDITION, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.WEAK_FOOT_ACC, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.WEAK_FOOT_FREQ, parseInt(values[col++]));
			Stats.setValue(of, player, Stats.CONSISTENCY, parseInt(values[col++]));

			for (Stat s : Stats.ABILITY_SPECIAL) {
				Stats.setValue(of, player, s, parseInt(values[col++]));
			}

			// Nationality string maps to integer?
			String natStr = values[col++];
			// Stats.setValue(of, player, Stats.NATIONALITY, ???);

			// The rest (international status, etc) we could skip for now,
			// it needs more advanced mapping

			return true;
		} catch (NumberFormatException e) {
			log.warn("Invalid number for player {} at column {}", player, col, e);
			return false;
		}
	}

	private int parseInt(String val) {
		if (Strings.isEmpty(val)) return 0;
		return Integer.parseInt(val.trim());
	}

	private int parseChar(String val) {
		val = val.trim();
		if (Strings.isEmpty(val)) return 0;
		char c = val.charAt(0);
		if (c >= 'A' && c <= 'C') {
			return c - 'A';
		}
		return 0;
	}
}
