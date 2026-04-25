package editor.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public final class UIUtil {
	private static final Logger log = LoggerFactory.getLogger(UIUtil.class);

	private UIUtil() {
	}

	public static final Color GRAY80 = new Color(0xCC, 0xCC, 0xCC);
	public static final Color LIGHT_YELLOW = new Color(255, 255, 224);

	public static final Color CHARTREUSE0 = new Color(183, 255, 0);


	public static final String DIALOG = "Dialog";
	//public static final String DIALOG_INPUT = "DialogInput";
	public static final String SANS_SERIF = "SansSerif";
	//public static final String SERIF = "Serif";
	//public static final String MONOSPACED = "Monospaced";


	public static void javaUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			log.warn(e.toString());
		}
	}

	public static void systemUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			// Remove the manual font scaling that didn't work well

			// Scale default Swing properties for higher resolution displays
			int resolution = Toolkit.getDefaultToolkit().getScreenResolution();
			float scaleFactor = resolution / 96.0f;
			if (scaleFactor > 1.0f) {
				Font font = new Font(SANS_SERIF, Font.PLAIN, Math.round(12 * scaleFactor));
				javax.swing.plaf.FontUIResource fontRes = new javax.swing.plaf.FontUIResource(font);

				UIManager.put("Menu.margin", new javax.swing.plaf.InsetsUIResource(4, 8, 4, 8));
				UIManager.put("MenuItem.margin", new javax.swing.plaf.InsetsUIResource(4, 8, 4, 8));

				// Make scrollbars thicker on High DPI
				UIManager.put("ScrollBar.width", Math.round(17 * scaleFactor));

				// Combo boxes
				UIManager.put("ComboBox.font", fontRes);
				UIManager.put("List.font", fontRes);
				UIManager.put("Label.font", fontRes);
				UIManager.put("CheckBox.font", fontRes);
				UIManager.put("Button.font", fontRes);
				UIManager.put("TextField.font", fontRes);
				UIManager.put("Menu.font", fontRes);
				UIManager.put("MenuItem.font", fontRes);

				int listRowHeight = Math.round(18 * scaleFactor);
				UIManager.put("List.rowHeight", listRowHeight);

				java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
				while (keys.hasMoreElements()) {
					Object key = keys.nextElement();
					Object value = UIManager.get(key);
					if (value instanceof javax.swing.plaf.FontUIResource) {
						UIManager.put(key, fontRes);
					}
					// Optional: adjust default size of things like scrollbars and combo boxes if needed
					// based on the scaleFactor here.
				}
			}
		} catch (Exception e) {
			log.warn(e.toString());
		}
	}

	public static String htmlColor(Color c) {
		return (null == c) ? "null" : String.format("#%08X", c.getRGB());
	}

}
