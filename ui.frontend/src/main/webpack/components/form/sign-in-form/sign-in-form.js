import jQuery from "jquery";

jQuery(function($) {
    "use strict";

    // Add error indicator style to the Sign In form
    (function() {
        const form = $('#kbank-sign-in-form');
        if(form) {
            const urlParams = new URLSearchParams(window.location.search);
            if(urlParams && urlParams.has('j_reason') && urlParams.get('j_reason') == 'invalid_login') {
                form.find('[name=j_username],[name=j_password]').addClass('cmp-form-text__text--error');
            }
        }
    })();

    const options = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    $(document).on('click', '#sign-in', function(event) {
        event.preventDefault();
        let form = $(this).closest('form')[0];
        let formData = new FormData(form);
        let actionUrl = $(form).attr('action');
        let url = new URL(actionUrl, window.location.origin);
        const errorBar = document.getElementById('errorBar');
        errorBar.style.display = 'none';

        // Append form data as query parameters
        for (let [key, value] of formData.entries()) {
            url.searchParams.append(key, value);
        }
        const options = {
            method: 'POST',
        };
        fetch(url, options)
            .then(response => response.json())
            .then(response => {
                console.log(response);
                if(response.status && response.status === 'error') {
                    const errorBar = document.getElementById('errorBar');
                    errorBar.textContent = response.message;
                    errorBar.style.display = 'block';
                } else {
                    window.location.href = response.redirect;
                }
            })
            .catch(err => {
                console.error(err);
                const errorBar = document.getElementById('errorBar');
                errorBar.textContent = err.error;
                errorBar.style.display = 'block';
            });
    });

    
    /* Add redirect to current page on the login  */
    $('body').on('kbank-modal-show', function() {
        const slingRedirectInput = $('#kbank-sign-in-form input[name="sling.auth.redirect"]');
        if(slingRedirectInput) {
            slingRedirectInput.val(window.location.pathname);
        }
    });

});
