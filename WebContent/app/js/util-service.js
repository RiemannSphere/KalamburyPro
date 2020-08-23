/**
 * @author Maciej Szabała
 */

const Util = {
    APP_NAME: 'KalamburyPro',
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
    },
    ROUTE: {
        Login2Game: 'app/html/game.html',
        Game2Login: '../../index.html'
    },
    TOKEN_HEADER: 'X-Token'
};

// Current state of a user
const UserInfo = {
    USERNAME: '',
    IS_DRAWING: false
};

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