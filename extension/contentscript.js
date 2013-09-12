function parseTask(url, sender, sendResponse) {
    if (sender.tab)
        return;
    console.log('hi');
    chrome.runtime.sendMessage('yandex' + '\n' + document.body.innerHTML);
    console.log('hi again');
}

chrome.runtime.onMessage.addListener(parseTask);
