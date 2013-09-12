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
