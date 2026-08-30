import sys
import gi
import threading

gi.require_version('Gtk', '3.0')
try:
    gi.require_version('AppIndicator3', '0.1')
    from gi.repository import AppIndicator3 as appindicator
except ValueError:
    gi.require_version('AyatanaAppIndicator3', '0.1')
    from gi.repository import AyatanaAppIndicator3 as appindicator

from gi.repository import Gtk, GLib

def update_label(indicator, text):
    # Pass the text as the second argument (guide string) so GTK reserves layout width
    indicator.set_label(text, text)
    return False

def main():
    # Use a widely available system icon name
    indicator = appindicator.Indicator.new(
        "hardware_monitor",
        "utilities-system-monitor",
        appindicator.IndicatorCategory.HARDWARE
    )
    indicator.set_status(appindicator.IndicatorStatus.ACTIVE)

    # Initial placeholder text so space is reserved immediately on launch
    indicator.set_label("Initializing...", "00% 00°C 0000RPM")

    menu = Gtk.Menu()
    quit_item = Gtk.MenuItem(label="Quit")
    quit_item.connect("activate", lambda _: Gtk.main_quit())
    menu.append(quit_item)
    menu.show_all()
    indicator.set_menu(menu)

    def read_stdin():
        for line in sys.stdin:
            clean_line = line.strip()
            if clean_line:
                # Schedule GTK UI update on the main thread
                GLib.idle_add(update_label, indicator, clean_line)

    threading.Thread(target=read_stdin, daemon=True).start()
    Gtk.main()

if __name__ == "__main__":
    main()