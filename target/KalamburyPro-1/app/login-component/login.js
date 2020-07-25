class Credentials{
    constructor(username, password) {
        this.username = username;
        this.password = password;
    }
}

const TOKEN = 'X-Token';

function login() {
    const username = document.getElementById('username').value;
    const credentials = new Credentials(username, 'dummy');
    const url = buildApiUrl(Util.API.REST, Util.IP.LOCAL, 8080, Util.APP_NAME, Util.RES.LOGIN);
    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(credentials)
    })
    .then((response) => response.text())
    .then(data => {
        if(data != null) {
            window.localStorage.setItem(TOKEN, data);
            window.location.href = 'app/game-component/game.html';
        } else {
            console.error('Token was null.')
        }
    })
    .catch((error) => {
        console.error('Error:', error); 
    });

}