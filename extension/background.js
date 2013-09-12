function checkForValidUrl(tabId, changeInfo, tab) {
    if (/http:\/\/algorithm.contest.yandex.(ru|com)\/contest\/\d*\/problems.*/.test(tab.url)) {
        chrome.pageAction.show(tabId);
    } else {
        chrome.pageAction.hide(tabId);
    }
}

chrome.tabs.onUpdated.addListener(checkForValidUrl);

function parseTask(tab) {
    chrome.tabs.sendMessage(tab.id, tab.url);
}

chrome.pageAction.onClicked.addListener(parseTask);

function send(message, sender, sendResponse) {
    console.log('hi');
    if (!sender.tab)
        return;
    var xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:4243', true);
    xhr.setRequestHeader('Content-type', 'text/plain');
    xhr.send(message);
    window.setTimeout(reload, 100);
    console.log('hi again');
}

function reload() {
    window.location.reload();
}

chrome.runtime.onMessage.addListener(send);
