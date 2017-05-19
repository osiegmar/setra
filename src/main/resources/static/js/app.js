function setTooltip(btn, message) {
    $(btn).tooltip('hide')
        .attr('data-original-title', message)
        .tooltip('show');
}

function hideTooltip(btn) {
    setTimeout(function() {
        $(btn).tooltip('hide');
    }, 1000);
}

$(function () {
    $('.clipboard-btn').tooltip({
        trigger: 'click',
        placement: 'bottom'
    });

    var clipboard = new Clipboard('.clipboard-btn');

    clipboard.on('success', function (e) {
        setTooltip(e.trigger, 'Copied!');
        hideTooltip(e.trigger);
    });

    clipboard.on('error', function (e) {
        setTooltip(e.trigger, 'Failed!');
        hideTooltip(e.trigger);
    });
    
    $(document).on('submit', 'form', function(event) {
        var abortSend = function() {
            $.unblockUI();
            history.go(0); // reset page
        };
        $.blockUI({
            message: 'Please wait .. click to ABORT',
            overlayCSS: { backgroundColor: '#00f' },
            onOverlayClick: abortSend
            }); 
        setTimeout($.unblockUI, 10000); 
    }); 
    
});
