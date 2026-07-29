/**
 * JSON to CSV Converter Skill
 * 
 * This module provides functionality to convert JSON data (array of objects) into CSV format.
 * It handles nested objects by flattening them with dot notation, manages missing fields,
 * and escapes special characters for CSV compliance.
 */

const fs = require('fs');
const path = require('path');

/**
 * Escapes a value for CSV format.
 * If the value contains commas, newlines, or double quotes, it wraps the value in quotes
 * and escapes internal double quotes by doubling them.
 * 
 * @param {any} value - The value to escape.
 * @returns {string} - The escaped CSV string representation.
 */
function escapeCSVValue(value) {
    if (value === null || value === undefined) {
        return '';
    }
    
    const str = String(value);
    
    // Check if escaping is needed (contains comma, newline, carriage return, or double quote)
    const needsQuotes = /[,"\r\n]/.test(str);
    
    if (needsQuotes) {
        // Escape double quotes by doubling them
        const escaped = str.replace(/"/g, '""');
        return `"${escaped}"`;
    }
    
    return str;
}

/**
 * Flattens a nested object into a single-level object with dot notation keys.
 * 
 * @param {object} obj - The object to flatten.
 * @param {string} prefix - The current key prefix (used in recursion).
 * @returns {object} - The flattened object.
 */
function flattenObject(obj, prefix = '') {
    const result = {};
    
    if (typeof obj !== 'object' || obj === null) {
        return { [prefix]: obj };
    }
    
    // Handle arrays by converting them to string or flattening if needed
    if (Array.isArray(obj)) {
        return { [prefix]: JSON.stringify(obj) };
    }
    
    for (const key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
            const newKey = prefix ? `${prefix}.${key}` : key;
            const value = obj[key];
            
            if (typeof value === 'object' && value !== null) {
                const flattened = flattenObject(value, newKey);
                Object.assign(result, flattened);
            } else {
                result[newKey] = value;
            }
        }
    }
    
    return result;
}

/**
 * Converts a JSON array of objects to a CSV string.
 * 
 * @param {Array<object>} jsonData - The JSON data to convert.
 * @param {object} options - Optional configuration.
 * @param {string} options.delimiter - The CSV delimiter (default: ',').
 * @param {boolean} options.includeHeader - Whether to include the header row (default: true).
 * @returns {string} - The resulting CSV string.
 */
function jsonToCsv(jsonData, options = {}) {
    const {
        delimiter = ',',
        includeHeader = true
    } = options;
    
    if (!Array.isArray(jsonData)) {
        throw new Error('Input data must be an array of objects.');
    }
    
    if (jsonData.length === 0) {
        return includeHeader ? '' : '';
    }
    
    // Flatten all objects to determine all possible keys
    const flattenedData = jsonData.map(item => flattenObject(item));
    
    // Get all unique keys from all flattened objects
    const allKeys = new Set();
    flattenedData.forEach(item => {
        Object.keys(item).forEach(key => allKeys.add(key));
    });
    
    const headers = Array.from(allKeys);
    
    // Build CSV lines
    const lines = [];
    
    if (includeHeader) {
        lines.push(headers.map(h => escapeCSVValue(h)).join(delimiter));
    }
    
    flattenedData.forEach(item => {
        const row = headers.map(header => escapeCSVValue(item[header] ?? '')).join(delimiter);
        lines.push(row);
    });
    
    return lines.join('\n');
}

/**
 * Main entry point for the skill.
 * Processes input data, performs JSON to CSV conversion, and returns a standardized result.
 * 
 * @param {object} inputData - The input data containing JSON to convert.
 * @param {Array<object>|string} inputData.json - The JSON data (array of objects) or a JSON string.
 * @param {object} [inputData.options] - Conversion options.
 * @param {string} [inputData.options.delimiter] - CSV delimiter (default: ',').
 * @param {boolean} [inputData.options.includeHeader] - Include header row (default: true).
 * @returns {object} - Standardized result object with status, message, and output.
 */
function main(inputData) {
    const startTime = Date.now();
    const logPrefix = `[JSON-to-CSV] ${new Date().toISOString()}`;
    
    console.log(`${logPrefix} Processing request...`);
    
    try {
        // Validate input
        if (!inputData || typeof inputData !== 'object') {
            throw new Error('Invalid input: Expected an object with "json" property.');
        }
        
        let jsonData = inputData.json;
        
        // Parse JSON string if provided
        if (typeof jsonData === 'string') {
            console.log(`${logPrefix} Parsing JSON string...`);
            try {
                jsonData = JSON.parse(jsonData);
            } catch (parseError) {
                throw new Error(`Failed to parse JSON string: ${parseError.message}`);
            }
        }
        
        // Validate that jsonData is an array
        if (!Array.isArray(jsonData)) {
            throw new Error('Invalid JSON data: Expected an array of objects.');
        }
        
        // Validate options
        const options = inputData.options || {};
        if (typeof options.delimiter !== 'string' || options.delimiter.length !== 1) {
            options.delimiter = ',';
        }
        if (typeof options.includeHeader !== 'boolean') {
            options.includeHeader = true;
        }
        
        console.log(`${logPrefix} Converting ${jsonData.length} records to CSV...`);
        
        // Perform conversion
        const csvResult = jsonToCsv(jsonData, options);
        
        const duration = Date.now() - startTime;
        console.log(`${logPrefix} Conversion completed successfully in ${duration}ms.`);
        
        return {
            status: 'success',
            message: `Successfully converted ${jsonData.length} records to CSV format.`,
            output: {
                csv: csvResult,
                recordCount: jsonData.length,
                fieldCount: csvResult.split('\n')[0]?.split(options.delimiter || ',').length || 0,
                processingTimeMs: duration
            }
        };
        
    } catch (error) {
        console.error(`${logPrefix} Error occurred: ${error.message}`);
        
        return {
            status: 'error',
            message: error.message,
            output: null
        };
    }
}

// Export for module usage
module.exports = {
    main,
    jsonToCsv,
    flattenObject,
    escapeCSVValue
};

// Example usage when run directly (for testing)
if (require.main === module) {
    const testInput = {
        json: [
            {
                id: 1,
                name: "Alice",
                address: {
                    city: "New York",
                    zip: "10001"
                },
                tags: ["developer", "nodejs"]
            },
            {
                id: 2,
                name: "Bob",
                address: {
                    city: "Los Angeles",
                    zip: "90001"
                },
                tags: ["designer"]
            },
            {
                id: 3,
                name: 'Charlie "The Builder"',
                address: {
                    city: "Chicago, IL",
                    zip: "60601"
                }
            }
        ],
        options: {
            delimiter: ',',
            includeHeader: true
        }
    };
    
    const result = main(testInput);
    console.log('\n=== Result ===');
    console.log(JSON.stringify(result, null, 2));
    
    if (result.status === 'success') {
        console.log('\n=== CSV Output ===');
        console.log(result.output.csv);
    }
}