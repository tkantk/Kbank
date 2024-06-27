import jQuery from "jquery";

jQuery(function($) {
    "use strict";

    (function() {
        const currentUserUrl = $('.kbank-sign-in-buttons').data('current-user-url'),
            signIn = $('[href="#sign-in"]'),
            signOut = $('[href="#sign-out"]'),
            greetingLabel = $('#kbankGreetingLabel'),
            greetingText = greetingLabel.text(),
            body = $('body');

        $.getJSON(currentUserUrl + "?nocache=" + new Date().getTime(), function(currentUser) {
            const isAnonymous = 'anonymous' === currentUser.authorizableId;

            if(isAnonymous) {
                signIn.show();
                body.addClass('anonymous');
            } else {
                signOut.show();
                greetingLabel.text(greetingText + ", " + currentUser.name);
                greetingLabel.show();
            }
        });
    })();
});
