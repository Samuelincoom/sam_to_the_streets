# SAM for the streets

Ok SES guys. This in Kotlin is for the actual app structure, the UI you tap on (buttons, graphs, menus), and handling how the data files like `.csv` or `.sam` load locally on your phone without sending them anywhere. Kotlin does the heavy lifting of reading your data, rendering the Vico Charts on screen, and keeping the app fast.

And this in Java is underneath the hood to crunch the backend parsing and bridge some libraries like Apache POI, making sure those chunky rows of energy data and load profiles load without crashing the app. 

### Why is it offline?
Because energy data should stay on your phone. This app uses a local LiteRT-LM (Gemma) model so you can actually ask questions about your datasets directly, offline. 

If you are a Python wizard, there is a built-in Python generator to help spin up script templates for your datasets so you can export to a laptop later. But for visualizing and understanding class projects fast: just open the file here and read the graph.
