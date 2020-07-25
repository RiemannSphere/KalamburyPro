const Util = {
    APP_NAME: 'KalamburyPro-1',
    API: {
        REST: 'http',
        WS: 'ws'
    },
    RES: {
        DRAW: 'draw',
        CHAT: 'chat',
        LOGIN: 'rest/login'
    },
    IP: {
        LOCAL: 'localhost'
    }
}

/**
 * Use Util object to build URL easier.
 * @param {string} api 
 * @param {string} ip 
 * @param {number} port 
 * @param {string} app 
 * @param {string} endpoint 
 */
function buildApiUrl(api, ip, port, app, endpoint) {
    return `${api}://${ip}:${port}/${app}/${endpoint}`;
}