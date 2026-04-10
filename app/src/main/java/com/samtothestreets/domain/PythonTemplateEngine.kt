package com.samtothestreets.domain

import com.samtothestreets.data.entity.ProjectCase

class PythonTemplateEngine {

    fun generateSnippet(query: String, projectCase: ProjectCase?, advanced: Boolean): Pair<String, String> {
        val q = query.lowercase()
        val cName = projectCase?.title ?: "local_dataset.csv"
        
        return when {
            q.contains("line") || q.contains("trend") -> Pair(
                "import pandas as pd\nimport matplotlib.pyplot as plt\n\ndf = pd.read_csv('$cName')\ndf.plot(kind='line')\nplt.title('Time Series Data')\nplt.show()",
                "Loads your generic dataset into Python and automatically plots all numeric columns against the index as a Line chart."
            )
            q.contains("bar") || q.contains("compare") -> Pair(
                "import pandas as pd\nimport matplotlib.pyplot as plt\n\ndf = pd.read_csv('$cName')\ndf.plot(kind='bar')\nplt.title('Categorical Comparison')\nplt.show()",
                "Uses pandas built-in wrapper to render a bar chart, ideal when your dataset relies on Strings/Categories interacting with numerics."
            )
            q.contains("scatter") || q.contains("correlation") -> Pair(
                "import pandas as pd\nimport matplotlib.pyplot as plt\n\ndf = pd.read_csv('$cName')\ndf.plot.scatter(x=df.columns[0], y=df.columns[1])\nplt.title('Scatter Plot')\nplt.show()",
                "Scatter plots help discover correlations between two numeric variables structurally."
            )
            else -> Pair(
                "import pandas as pd\n\ndf = pd.read_csv('$cName')\nprint(df.describe())",
                "By default, pandas `.describe()` gives you the exact same Min, Max, and Average summary statistics displayed on the app's insight cards."
            )
        }
    }
}
