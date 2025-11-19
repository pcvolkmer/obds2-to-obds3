const rspack = require('@rspack/core');

module.exports = {
    entry: {
        main: './index.js',
    },
    output: {
        path: '../resources/static/',
        chunkFilename: '[id].js'
    }
};
