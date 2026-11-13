const TEMPERATURE_TIERS = [
    {key: 'idle', label: 'Idle', bound: 'temperature_idle', colorToken: '--temp-tier-idle'},
    {key: 'normal', label: 'Normal load', bound: 'temperature_normal_load', colorToken: '--temp-tier-normal'},
    {key: 'heavy', label: 'Heavy load', bound: 'temperature_heavy_load', colorToken: '--temp-tier-heavy'},
    {key: 'warning', label: 'Warning', bound: 'temperature_warning', colorToken: '--temp-tier-warning'},
    {key: 'throttling', label: 'Throttling', bound: 'temperature_throttling', colorToken: '--temp-tier-throttling'}
];

let sensorConfigs = {};
let labelsEnabled = false;

function setSensorConfigs(sensors, showLabels) {
    sensorConfigs = sensors || {};
    labelsEnabled = showLabels === true;
}

function areTierLabelsEnabled() {
    return labelsEnabled;
}

function getSensorConfig(sensorID) {
    return sensorConfigs[sensorID];
}

function parseValue(value) {
    if (typeof value === 'number') return Number.isFinite(value) ? value : null;
    const match = String(value ?? '').match(/-?\d+(\.\d+)?/);
    return match ? parseFloat(match[0]) : null;
}

/**
 * Returns the temperature tier a reading (or a time-over-temp threshold) falls
 * into, or null when the sensor has no tiers configured/enabled.
 */
function getTemperatureTier(sensorID, value) {
    const config = sensorConfigs[sensorID];
    if (!config || !config.showTempTiers) return null;

    const number = parseValue(value);
    if (number === null) return null;

    let highestConfigured = null;
    for (const tier of TEMPERATURE_TIERS) {
        const bound = parseValue(config[tier.bound]);
        if (bound === null || bound <= 0) continue;

        highestConfigured = tier;
        if (number <= bound) return tier;
    }

    return highestConfigured;
}

/**
 * Returns only the tiers that have a usable threshold configured for a sensor,
 * paired with the parsed bound. Used by the threshold tooltip.
 */
function getConfiguredTiers(sensorID) {
    const config = sensorConfigs[sensorID];
    if (!config || !config.showTempTiers) return [];

    const tiers = [];
    for (const tier of TEMPERATURE_TIERS) {
        const bound = parseValue(config[tier.bound]);
        if (bound === null || bound <= 0) continue;
        tiers.push({...tier, bound});
    }
    return tiers;
}

function tierColor(tier) {
    return tier ? `var(${tier.colorToken})` : null;
}

function getSensorUnit(sensorID) {
    return sensorConfigs[sensorID]?.unit || '';
}

/**
 * Formats a raw sensor reading for display. When the sensor config has
 * showPercentage enabled, the unit is stripped and the reading is shown as
 * a percentage of percentageMaxValue, e.g. "32 (50%)".
 */
function formatSensorValue(sensorID, value) {
    const config = sensorConfigs[sensorID];
    const str = String(value ?? '');
    if (!config || !config.showPercentage) return str;

    const num = parseValue(str);
    if (num === null) return str;

    // const numericPart = str.match(/-?\d+(\.\d+)?/)?.[0] ?? str;
    const max = parseValue(config.percentageMaxValue);
    if (max === null || max <= 0) return str;

    return `${str} (${Math.round((num / max) * 100)}%)`;
}

export {
    TEMPERATURE_TIERS,
    setSensorConfigs,
    areTierLabelsEnabled,
    getSensorConfig,
    getTemperatureTier,
    getConfiguredTiers,
    tierColor,
    getSensorUnit,
    formatSensorValue,
    parseValue
};
