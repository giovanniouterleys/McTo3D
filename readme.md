<div align="center">

# <img src="logo-mcto3d.png" alt="McTo3D Logo" width="200"/>
<br>
<b>McTo3D</b>
<br>
<i>Turn Minecraft into a powerful CAD tool for 3D printing & bring 3D models into your world.</i>

<br>

> **⚠️ Public Beta:** This mod is currently in active development. While the core features work, please encounter bugs and back up your worlds.

</div>

---

## 📖 About

Hi! I'm a solo developer passionate about 3D printing and Minecraft. I created **McTo3D** to bridge the gap between these two worlds, allowing you to easily export your in-game creations as printable 3D files, and import real 3D models back into the game as blocks.

Currently developed for **Fabric**, this mod aims to be the simplest workflow from blocks to slicer, and from slicer to blocks.

---

## ✨ Features (v0.2.0-beta)

### 🛠️ Intuitive Selection
* **Wand Selection:** Just grab a **Golden Hoe**. Left-click for Position 1, Right-click for Position 2.
* **Commands:** Use `/pos1` and `/pos2` for precise targeting at your feet.
* **Visual Feedback:** Selection particles appear to show you exactly what will be exported.

### 📐 Smart Manipulation
* **Vertical Expansion:** Use `/expand <amount>` (e.g., `/expand 10` to go up).
* **Precise Scaling:** Define exactly how big your export will be in the real world using `/setscale <mm>`.
    * Example: `/setscale 10` means **1 block = 10mm (1cm)**.

### 💾 Powerful Export Engine
The export engine supports multiple workflows:
* **STL (Raw):** Optimized binary STL for structural 3D printing.
* **OBJ (Color):** Exports geometry with material colors (perfect for multi-color printing).
* **OBJ (Textures):** Full texture support for rendering or complex prints.
* **Diagonal Fixing:** Automatically fixes non-manifold geometry (diagonal blocks) so your slicer doesn't complain.

### 🤖 AI & Import System (New!)
* **Import 3D Models:** Load any `.obj` file from your computer into Minecraft using `/import3d <filename> <scale>`.
* **Generative AI:** Generate 3D models from text prompts directly inside Minecraft using Nvidia Trellis AI!
    * Command: `/import3d ai <scale> <prompt>`
    * *Requires an API Key.*
* **Hologram Placement:** Preview your imported model as a ghost hologram before placing it.
    * **Rotate:** Use `Right Arrow` key.
    * **Distance:** Use `Up/Down Arrow` keys.
    * **Place:** Press `Enter`.

---

## 📥 Installation

1.  Install **Fabric Loader** for your Minecraft version (1.20.5+).
2.  **Important:** Download and install **Fabric API** (Required).
3.  Download the latest `McTo3D-x.x.x.jar` from the releases tab.
4.  Drop the `.jar` file into your `.minecraft/mods` folder.
5.  Launch the game!

---

## 🚀 Quick Start Guide

### 📤 Exporting (Minecraft -> 3D Print)
1.  Equip a **Golden Hoe**.
2.  Select two corners of your build (Left/Right click).
3.  (Optional) Set the scale: `/setscale 10` (1 block = 1cm).
4.  Export: `/export3d my_project`.
5.  Find your files in `.minecraft/exports/my_project/`.

### 📥 Importing (3D Model -> Minecraft)
1.  **Local File:** Place your `.obj` file in `.minecraft/imports/`.
    * Run: `/import3d my_model.obj 2.0` (Scale 2.0).
2.  **AI Generation:**
    * Configure API Key: `/mcto3d apikey <your_nvidia_key>`.
    * Run: `/import3d ai 2.0 "a medieval castle"`.
3.  **Placement:**
    * Move the hologram with Arrow Keys.
    * Press **Enter** to build it!

---

## 🎥 Tutorials & Demos

Need a visual guide? Want to see how to install the mod or watch it in action?
Check out my TikTok for step-by-step tutorials, dev logs, and print showcases!

👉 **[Watch the Tutorials on TikTok (@gotr07)](https://www.tiktok.com/@gotr07?lang=en)**

---

## 🔮 Roadmap

* 🚀 **Export Optimization:** Making file generation even faster.
* 🧩 **Schematic Support:** Support for .litematica or .schem files.
* ✨ **GUI:** A proper graphical interface for settings instead of commands.

---

## 🐛 Issues & Feedback

As this is a Beta release, your feedback is crucial. If you find a bug or have a suggestion, please open an issue in the **Issues** tab on GitHub.

**License:** GNU GPLv3  
*Happy Printing!*