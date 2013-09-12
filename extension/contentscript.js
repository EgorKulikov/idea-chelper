function parseTask(url, sender, sendResponse) {
    console.log('hi');
    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:4243', true);
    xhr.setRequestHeader('Content-type', 'text/plain');
    xhr.send('yandex' + '\n' + document.body.innerHTML);
    console.log('hi again');
}

chrome.runtime.onMessage.addListener(parseTask);
