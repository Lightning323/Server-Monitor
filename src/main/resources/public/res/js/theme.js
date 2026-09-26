const THEME_ATTRIBUTE = 'data-bs-theme';
const THEME_CHANGE_EVENT = 'themechange';

const tokens = new Map();
const listeners = new Set();

// The bootstrap script in index.html owns the attribute, this only observes it.
document.addEventListener(THEME_CHANGE_EVENT, () => {
    tokens.clear();
    listeners.forEach(listener => listener(isDark()));
});

function isDark() {
    return document.documentElement.getAttribute(THEME_ATTRIBUTE) === 'dark';
}

/**
 * Reads a custom property off :root. Values are cached until the theme changes,
 * so the chart draw hooks can call this on every frame.
 */
function token(name) {
    if (!tokens.has(name)) {
        const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
        tokens.set(name, value);
    }
    return tokens.get(name);
}

function onThemeChange(listener) {
    listeners.add(listener);
}

/**
 * @returns {() => string} a getter uPlot calls on each draw, so a series picks up
 * the new palette as soon as the theme flips.
 */
function seriesColor(name) {
    return () => token(name);
}

/**
 * Parses a '#rgb'/'#rrggbb' custom property into an [r, g, b] triple.
 */
function parseColor(value) {
    const hex = value.replace('#', '');
    const full = hex.length === 3 ? hex.split('').map(c => c + c).join('') : hex;
    return [
        parseInt(full.substring(0, 2), 16),
        parseInt(full.substring(2, 4), 16),
        parseInt(full.substring(4, 6), 16)
    ];
}

export {token, onThemeChange, seriesColor, parseColor};
