import jQuery from "jquery";

jQuery(function($) {
    "use strict";

    // Add error indicator style to the Register form
    (function() {
        const form = $('#kbank-register-form');
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

    if (document.getElementById('kbank-register-form') && document.getElementById('kbank-register-form') != null) {
        document.getElementById('kbank-register-form').addEventListener('submit', function (event) {
            event.preventDefault();
            let formData = new FormData(this);
            let url = new URL(this.action);
            const successBar = document.getElementById('successBar');
            const errorBar = document.getElementById('errorBar');
            successBar.style.display = 'none';
            errorBar.style.display = 'none';

            // Append form data as query parameters
            for (let [key, value] of formData.entries()) {
                url.searchParams.append(key, value);
            }
            const options = {
                method: 'POST',
            };
            showLoader();
            fetch(url, options)
                .then(response => response.json())
                .then(response => {
                    console.log(response);
                    hideLoader();
                    if (response.message) {
                        successBar.textContent = response.message;
                        successBar.style.display = 'block';
                        errorBar.style.display = 'none';
                    } else if (response.error) {
                        const errorBar = document.getElementById('errorBar');
                        errorBar.textContent = response.error;
                        errorBar.style.display = 'block';
                    }
                })
                .catch(err => {
                    console.error(err);
                    hideLoader();
                    const errorBar = document.getElementById('errorBar');
                    errorBar.textContent = err.error;
                    errorBar.style.display = 'block';
                });
        });
    }

    
    /* Add redirect to current page on the login  */
    $('body').on('kbank-modal-show', function() {
        const slingRedirectInput = $('#kbank-register-form input[name="sling.auth.redirect"]');
        if(slingRedirectInput) {
            slingRedirectInput.val(window.location.pathname);
        }
    });

    // Function to show the loader
    function showLoader() {
        document.getElementById('loader-overlay').style.display = 'flex';
    }

// Function to hide the loader
    function hideLoader() {
        document.getElementById('loader-overlay').style.display = 'none';
    }

});
