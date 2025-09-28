# Configuration file for the Sphinx documentation builder.
# AutoHeal Locator Documentation

# -- Project information -----------------------------------------------------
project = 'AutoHeal Locator'
copyright = '2024, AutoHeal Team'
author = 'AutoHeal Team'
release = '2.0.0'

# -- General configuration ---------------------------------------------------
extensions = [
    'myst_parser',
    'sphinx.ext.autodoc',
    'sphinx.ext.viewcode',
    'sphinx.ext.napoleon',
    'sphinx_design',
    'sphinx_copybutton',
    'sphinx.ext.intersphinx',
]

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store', 'site', '.github', '.claude', 'temp_backup', 'target']

# -- Options for HTML output -------------------------------------------------
html_theme = 'pydata_sphinx_theme'

html_theme_options = {
    "github_url": "https://github.com/yourusername/autoheal-locator",
    "use_edit_page_button": False,
    "show_toc_level": 3,
    "navbar_align": "left",
    "navbar_center": ["navbar-nav"],
    "navbar_persistent": ["search-button"],
    "primary_sidebar_end": ["indices"],
    "footer_start": ["copyright"],
    "footer_end": ["sphinx-version"],
    "secondary_sidebar_items": ["page-toc", "sourcelink"],
    "header_links_before_dropdown": 5,
    "navigation_with_keys": False,
    "show_prev_next": False,
    "article_header_start": ["breadcrumbs"],
    "article_header_end": [],
    "show_nav_level": 2,
    "navigation_depth": 4,
    "collapse_navigation": False,
    "navigation_expand_subsections": True,
    "icon_links": [
        {
            "name": "GitHub",
            "url": "https://github.com/yourusername/autoheal-locator",
            "icon": "fa-brands fa-github",
            "type": "fontawesome",
        },
        {
            "name": "Maven Central",
            "url": "https://maven-badges.herokuapp.com/maven-central/com.autoheal/autoheal-locator",
            "icon": "fa-solid fa-cube",
            "type": "fontawesome",
        },
        {
            "name": "JavaDoc",
            "url": "https://javadoc.io/doc/com.autoheal/autoheal-locator",
            "icon": "fa-solid fa-book",
            "type": "fontawesome",
        },
    ],
    "logo": {
        "image_light": "_static/logo.svg",
        "image_dark": "_static/logo.svg",
        "alt": "AutoHeal Locator Documentation",
    },
    "navigation_depth": 2,
    "collapse_navigation": True,
    "show_nav_level": 1,
    "article_footer_items": ["prev-next"],
}

html_context = {
    "github_user": "yourusername",
    "github_repo": "autoheal-locator",
    "github_version": "main",
    "doc_path": "docs",
}

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']

# Custom CSS files to include
html_css_files = [
    'custom.css',
]

html_title = "AutoHeal Locator"
html_short_title = "AutoHeal"

# Configure the global toctree for navigation
html_sidebars = {
    "**": ["navbar-logo", "search-field", "globaltoc.html", "sidebar-ethical-ads"]
}

# -- Options for MyST parser ------------------------------------------------
myst_enable_extensions = [
    "amsmath",
    "attrs_inline",
    "colon_fence",
    "deflist",
    "dollarmath",
    "fieldlist",
    "html_admonition",
    "html_image",
    "linkify",
    "replacements",
    "smartquotes",
    "strikethrough",
    "substitution",
    "tasklist",
]

myst_url_schemes = ("http", "https", "mailto")

# -- Options for copy button ------------------------------------------------
copybutton_prompt_text = r">>> |\.\.\. |\$ |In \[\d*\]: | {2,5}\.\.\.: | {5,8}: "
copybutton_prompt_is_regexp = True
copybutton_line_continuation_character = "\\"

# -- Options for intersphinx -------------------------------------------------
intersphinx_mapping = {
    "python": ("https://docs.python.org/3", None),
    "selenium": ("https://selenium-python.readthedocs.io/", None),
}

# -- Options for autodoc ----------------------------------------------------
autodoc_typehints = "description"
autodoc_typehints_description_target = "documented"