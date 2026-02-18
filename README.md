# Dobot-Gui 🤖  

A graphical user interface (GUI) for controlling the Dobot Magician robotic arm. Developed using Python, this tool aims to make robot manipulation intuitive and accessible without the need for constant coding.

# ✨ Features  
**User-Friendly Interface:** Control the robot via a clean dashboard.

**Real-time Control:** Jogging controls for X, Y, Z, and R axes.

**Coordinate Monitoring:** Displays real-time coordinate feedback from the robotic arm.

**Home Function:** Quick button to reset the robot to its Home Position.

**Easy Connectivity:** Connect seamlessly via Serial Port (COM Port).

# 🛠 Installation
**Clone the repository:**
```
git clone https://github.com/PetchTueksuban/Dobot-Gui.git  
cd Dobot-Gui  
```
**Install Required Libraries:**  
Ensure you have Python installed, then run:
```
pip install -r requirements.txt
```
**(Note: If requirements.txt is missing, ensure you have pydobot and your UI framework—like PyQt5 or Tkinter—installed.)**  

**Prepare DLL Files (Windows):**
Ensure the DobotDll.dll file is located in the root directory of the project for proper driver communication.

# 🚀 How to Use
Connect the Dobot Magician to your computer via USB.  

**Run the application:**
```
python main.py
```
Select the correct COM Port from the dropdown menu and click Connect.

Start controlling the robotic arm using the on-screen buttons!

# 📂 Project Structure
```main.py``` - The main entry point for the GUI application.

```dobot_api.py``` - API wrapper for robot communication.

```ui/``` - UI design files or layout scripts.

```resources/ ```- Assets such as icons and images.

**🤝 Contributing
Contributions are welcome! If you find a bug or have a feature request, please open an Issue or submit a Pull Request.**
