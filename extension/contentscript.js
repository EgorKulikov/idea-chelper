function parseTask(message, sender, sendResponse) {
    if (sender.tab)
        return;
    console.log(message);
    chrome.runtime.sendMessage(message + '\n' + document.body.innerHTML);
}

chrome.runtime.onMessage.addListener(parseTask);
