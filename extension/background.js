function checkForValidUrl(tabId, changeInfo, tab) {
    if (/^http:\/\/algorithm[.]contest[.]yandex[.](ru|com)\/contest\/\d*\/problems.*$/.test(tab.url) ||
        /^http:\/\/codeforces[.](ru|com)\/(contest|problemset|gym)\/\d*\/problem\/.+$/.test(tab.url) ||
        /^https:\/\/(www[.])?hackerrank[.]com\/(contests\/.+\/)?challenges\/[^/]+$/.test(tab.url)) {
        chrome.pageAction.show(tabId);
    } else {
        chrome.pageAction.hide(tabId);
    }
}

chrome.tabs.onUpdated.addListener(checkForValidUrl);

function parseTask(tab) {
    console.log(tab.url);
    if (/^http:\/\/algorithm[.]contest[.]yandex[.](ru|com)\/contest\/\d*\/problems.*$/.test(tab.url)) {
        chrome.tabs.sendMessage(tab.id, 'yandex');
    } else if (/^http:\/\/codeforces[.](ru|com)\/(contest|problemset|gym)\/\d*\/problem\/.+$/.test(tab.url)) {
        chrome.tabs.sendMessage(tab.id, 'codeforces');
    } else if (/^https:\/\/(www[.])?hackerrank[.]com\/(contests\/.+\/)?challenges\/[^/]+$/.test(tab.url)) {
        console.log('hackerrank');
        chrome.tabs.sendMessage(tab.id, 'hackerrank');
    }
}

chrome.pageAction.onClicked.addListener(parseTask);

function send(message, sender, sendResponse) {
    if (!sender.tab)
        return;
    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:4243', true);
    xhr.setRequestHeader('Content-type', 'text/plain');
    xhr.send(message);
    window.setTimeout(reload, 500);
}

function reload() {
    window.location.reload();
}

chrome.runtime.onMessage.addListener(send);
