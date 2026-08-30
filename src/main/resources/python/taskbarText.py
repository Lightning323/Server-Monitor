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

def send_action(action):
    print(action, flush=True)

def open_webapp(_):
    send_action("OPEN_WEBAPP")

def quit_monitor(_):
    send_action("QUIT")
    Gtk.main_quit()

def main():
    icon_path = sys.argv[1] if len(sys.argv) > 1 else "utilities-system-monitor"
    indicator = appindicator.Indicator.new(
        "server-monitor",
        icon_path,
        appindicator.IndicatorCategory.HARDWARE
    )
    # The Java launcher supplies the bundled Server Monitor icon as an absolute path.
    if len(sys.argv) > 1:
        indicator.set_icon_full(icon_path, "Server Monitor")
    indicator.set_status(appindicator.IndicatorStatus.ACTIVE)

    # Initial placeholder text so space is reserved immediately on launch
    indicator.set_label("Initializing...", "00% 00°C 0000RPM")

    menu = Gtk.Menu()
    open_item = Gtk.MenuItem(label="Open web app")
    open_item.connect("activate", open_webapp)
    menu.append(open_item)
    quit_item = Gtk.MenuItem(label="Quit")
    quit_item.connect("activate", quit_monitor)
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
