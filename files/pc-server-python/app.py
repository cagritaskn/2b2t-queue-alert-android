import tkinter as tk
from tkinter import filedialog, messagebox
from flask import Flask, jsonify
import threading
import os
import socket
from ttkthemes import ThemedTk
from tkinter import ttk
import signal
import sys
import pystray
from PIL import Image
import re
import datetime
import webbrowser
import shutil
import winreg

app = Flask(__name__)
selected_file = None
server_running = False
default_file = os.path.join(os.path.expanduser('~'), 'AppData', 'Roaming', '.minecraft', 'logs', 'latest.log')
flask_server = None
tray_icon = None
startup_txt_path = os.path.join('C:\\Program Files\\2B2T Queue Alert', 'startup.txt')
exe_path = os.path.join('C:\\Program Files\\2B2T Queue Alert', '2b2tqueueserver.exe')
checkbox_var = None

def get_resource_path(relative_path):
    try:
        base_path = sys._MEIPASS
    except Exception:
        base_path = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(base_path, relative_path)

def read_last_numeric_line(file_path):
    try:
        with open(file_path, 'r') as file:
            lines = file.readlines()
            if lines:
                last_line = lines[-1].strip()
                print(f"Last line read: '{last_line}'")  # Debugging

                match = re.search(r'Position in queue: (\d+)', last_line)
                if match:
                    numeric_value = match.group(1)
                else:
                    numeric_value = ''
                
                is_restarting = "restarting" in last_line.lower()
                if "[CHAT]" not in last_line:
                    numeric_value = ''
                    is_restarting = False

                if len(numeric_value) > 8:
                    numeric_value = numeric_value[6:-2]
                
                return numeric_value, is_restarting

    except Exception as e:
        print(f"Error reading file: {e}")
    return "No numeric value found", False

@app.route('/get-data', methods=['GET'])
def get_data():
    if selected_file and os.path.exists(selected_file):
        numeric_data, is_restarting = read_last_numeric_line(selected_file)
        return jsonify({'numeric_value': numeric_data, 'is_restarting': is_restarting})
    else:
        return jsonify({'numeric_value': 'File not found', 'is_restarting': False})

def start_flask_server():
    global server_running, flask_server
    server_running = True
    flask_server = threading.Thread(target=app.run, kwargs={'host': '0.0.0.0', 'port': 5000, 'use_reloader': False})
    flask_server.start()

def stop_flask_server():
    global server_running, flask_server
    server_running = False
    if flask_server and flask_server.is_alive():
        os.kill(os.getpid(), signal.SIGINT)
        flask_server.join()
    flask_server = None

def toggle_server():
    global server_running
    if server_running:
        stop_flask_server()
        update_status_label(False)
    else:
        start_flask_server()
        update_status_label(True)

def select_file():
    global selected_file
    current_file = selected_file
    if current_file and os.path.exists(default_file) and (current_file == default_file or not os.path.exists(current_file)):
        if not messagebox.askyesno("File Change", "Are you sure you want to select another file?"):
            return

    try:
        selected_file = filedialog.askopenfilename(
            initialdir=os.path.expanduser('~'),
            title="Select File",
            filetypes=(("Latest Log File", "latest.log"),)
        )
        update_file_status()
    except Exception as e:
        print(f"Error selecting file: {e}")

def get_ip_address():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 1))
        return s.getsockname()[0]
    except Exception as e:
        return "IP address not available"
    finally:
        s.close()

def set_default_file():
    global selected_file
    if os.path.exists(default_file):
        selected_file = default_file
        file_status_label.config(text="latest.log file found", foreground="#E55BB7", font=("Roboto", 10, "bold"))
        button.config(text="Choose another latest.log")
    else:
        file_status_label.config(text="Please select latest.log file", foreground="#E14F4F", font=("Roboto", 10, "bold"))
        selected_file = None
        button.config(text="Choose latest.log")
        create_file = messagebox.askyesno(
            "Create latest.log File",
            f"No latest.log file found in {default_file}. Do you want to create it?"
        )
        if create_file:
            try:
                with open(default_file, 'w') as file:
                    file.write("")
                selected_file = default_file
                file_status_label.config(text="latest.log file created", foreground="#E55BB7", font=("Roboto", 10, "bold"))
                button.config(text="Choose another latest.log")
            except Exception as e:
                print(f"Error creating file: {e}")
        update_file_status()

def update_file_status():
    if selected_file and os.path.exists(selected_file):
        file_status_label.config(text="latest.log file found", foreground="#E55BB7", font=("Roboto", 10, "bold"))
    else:
        file_status_label.config(text="Please select latest.log file", foreground="#E14F4F", font=("Roboto", 10, "bold"))

def update_status_label(running):
    if running:
        status_label.config(text="Server is running.", foreground="white", font=("Roboto", 10, "bold"))
        update_status_periodically()
    else:
        status_label.config(text="Server not started", foreground="white", font=("Roboto", 10, "bold"))

def update_status_periodically():
    dot_count = 0

    def status_update():
        nonlocal dot_count
        if dot_count == 0:
            status_label.config(text="Server is running.")
        elif dot_count == 1:
            status_label.config(text="Server is running..")
        elif dot_count == 2:
            status_label.config(text="Server is running...")
        dot_count = (dot_count + 1) % 3
        root.after(500, status_update)

    status_update()

def withdraw_window():
    root.withdraw()

def on_closing():
    if messagebox.askyesno("Confirm Quit", "Are you sure you want to quit? (The server will be terminated)"):
        stop_flask_server()
        root.destroy()
    else:
        root.deiconify()  # Restore window if user cancels

def restore_from_tray(icon=None, item=None):
    root.after(0, root.deiconify)
    icon.visible = True

def quit_app_from_menu(icon, item):
    if messagebox.askyesno("Confirm Quit", "Are you sure you want to quit? (The server will be terminated)"):
        stop_flask_server()
        root.quit()

def create_tray_icon():
    icon_image_path = get_resource_path('icon.ico')
    icon_image = Image.open(icon_image_path)

    global tray_icon
    tray_icon = pystray.Icon("name", icon_image, "2B2T Queue Alert")
    tray_icon.menu = pystray.Menu(
        pystray.MenuItem('Show the window', restore_from_tray),
        pystray.MenuItem('Quit', quit_app_from_menu)
    )
    tray_icon.run_detached()

def log_startup_message():
    if selected_file and os.path.exists(selected_file):
        try:
            with open(selected_file, 'a') as file:
                current_time = datetime.datetime.now().strftime("[%H:%M:%S]")
                message = f"{current_time} : 2B2T Queue Alert System Started.\n"
                file.write(message)
                print(f"Logged startup message: '{message.strip()}'")
        except Exception as e:
            print(f"Error logging startup message: {e}")

def show_help():
    help_text = (
        "© 2024 Çağrı Taşkın. 2B2T Queue Alert for 2B2T Server.\n\n"
        "This application monitors the 2B2T queue position from the latest.log file and provides a REST API to access the data.\n\n"
        "Do you want to visit github page for more information ?\n"
        "https://github.com/cagritaskn/2b2t-queue-alert-android"
    )
    response = messagebox.askyesno(
        "Help and Credits",
        help_text
    )
    if response:
        webbrowser.open("https://github.com/cagritaskn/2b2t-queue-alert-android")

# Function to handle reading and writing to the startup.txt file
def handle_startup_file(write_value=None):
    os.makedirs(os.path.dirname(startup_txt_path), exist_ok=True)
    if write_value is not None:
        with open(startup_txt_path, 'w') as f:
            f.write(write_value)
    else:
        if not os.path.exists(startup_txt_path):
            handle_startup_file("false")
        with open(startup_txt_path, 'r') as f:
            return f.read().strip()
    return "false"

# Add registry key for the user specified startup
def handle_windows_startup_registry(add_to_startup):
    key = r"SOFTWARE\Microsoft\Windows\CurrentVersion\Run"
    if add_to_startup:
        try:
            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, key, 0, winreg.KEY_SET_VALUE) as reg_key:
                winreg.SetValueEx(reg_key, "2B2TQueueServer", 0, winreg.REG_SZ, exe_path)
        except Exception as e:
            print(f"Error adding to startup: {e}")
    else:
        try:
            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, key, 0, winreg.KEY_SET_VALUE) as reg_key:
                winreg.DeleteValue(reg_key, "2B2TQueueServer")
        except Exception as e:
            print(f"Error removing from startup: {e}")

def toggle_startup_checkbox():
    if checkbox_var.get() == 1:
        handle_startup_file("true")
        handle_windows_startup_registry(True)
    else:
        handle_startup_file("false")
        handle_windows_startup_registry(False)

# Copy the exe for user specified startup with Windows
def copy_exe_to_program_files():
    exe_path = os.path.join('C:\\Program Files\\2B2T Queue Alert', '2b2tqueueserver.exe')
    source_path = sys.executable  # Bu, çalışmakta olan Python betiğinizin yoludur.
    
    try:
        os.makedirs(os.path.dirname(exe_path), exist_ok=True)
        if not os.path.exists(exe_path):
            shutil.copy(source_path, exe_path)
            print(f"Copied executable to {exe_path}")  # Debugging
        else:
            print("Executable already exists in program files folder")  # Debugging
    except Exception as e:
        print(f"Error copying executable to program files: {e}")

def main():
    global file_status_label, button, status_label, root, quit_button, help_button, hide_button, checkbox_var, checkbox

    try:
        root = ThemedTk(theme="breeze")
        root.title("2B2T Queue Alert")
        root.geometry("400x320")
        root.protocol("WM_DELETE_WINDOW", on_closing)
        root.resizable(False, False)
        
        icon_image_path = get_resource_path('icon.ico')
        root.iconbitmap(icon_image_path)
        
        copy_exe_to_program_files()

        style = ttk.Style()
        style.configure("TButton", font=("Roboto", 10, "bold"), padding=5, background="#444444", foreground="black")
        style.configure("TLabel", font=("Roboto", 10, "bold"), background="#444444", foreground="white")
        style.configure("TCheckbutton",
                background="#444444",  # Arka plan rengi
                foreground="white")    # Metin rengi

        canvas = tk.Canvas(root, width=400, height=320, bg="#444444")
        canvas.pack()

        file_status_label = ttk.Label(root, text="Checking...", background="#444444", font=("Roboto", 10, "bold"))
        canvas.create_window(200, 30, window=file_status_label)

        button = ttk.Button(root, text="Choose latest.log", command=select_file)
        canvas.create_window(200, 70, window=button)

        ip_address = get_ip_address()
        ip_label = ttk.Label(root, text=f"IP: {ip_address}", background="#444444", foreground="#DADADA", font=("Roboto", 12, "bold"))
        ip_label.place(x=200, y=130, anchor="center")

        port_label = ttk.Label(root, text="Port: 5000", background="#444444", foreground="#DADADA", font=("Roboto", 10, "bold"))
        port_label.place(x=200, y=150, anchor="center")

        status_label = ttk.Label(root, text="Server not started", foreground="white", background="#444444", font=("Roboto", 10, "bold"))
        status_label.place(x=200, y=110, anchor="center")

        quit_button = ttk.Button(root, text="Quit", command=on_closing)
        canvas.create_window(200, 260, window=quit_button)
        
        hide_button = ttk.Button(root, text="Minimize to Tray", command=withdraw_window)
        canvas.create_window(200, 180, window=hide_button)  # Positioned above the Quit button

        help_button = ttk.Button(root, text="Help and Info", command=show_help)
        canvas.create_window(200, 220, window=help_button)

        # Checkbox for "Run on Windows Startup"
        checkbox_var = tk.IntVar()
        checkbox = ttk.Checkbutton(root, text="Run on Windows Startup", style="TCheckbutton", variable=checkbox_var, command=toggle_startup_checkbox)
        canvas.create_window(200, 295, window=checkbox)

        # Set checkbox state based on startup.txt
        startup_value = handle_startup_file()
        if startup_value == "true":
            checkbox_var.set(1)
            handle_windows_startup_registry(True)
        else:
            checkbox_var.set(0)
            handle_windows_startup_registry(False)

        set_default_file()
        log_startup_message()
        toggle_server()
        copy_exe_to_program_files()
        create_tray_icon()
        root.mainloop()

    except Exception as e:
        print(f"Error starting GUI: {e}")

if __name__ == "__main__":
    main()