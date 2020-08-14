function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const credentials = new Credentials(username, password);
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
            if (data != null) {
                window.localStorage.setItem(Util.TOKEN_HEADER, data);
                window.location.href = Util.ROUTE.Login2Game;
            } else {
                console.error('Token was null.')
            }
        })
        .catch((error) => {
            console.error('Error:', error);
        });

}