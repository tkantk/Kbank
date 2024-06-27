import jQuery from "jquery";

jQuery(function($) {
    "use strict";

    const options = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    $(document).on('click', '#admin-form', function(event) {
        event.preventDefault();
        let form = $(this).closest('form')[0];
        let formData = new FormData(form);
        let actionUrl = $(form).attr('action');
        let type = $(form).attr('j_type');
        let url = new URL(actionUrl, window.location.origin);
        const errorBar = document.getElementById('errorBar');
        errorBar.style.display = 'none';

        // Append form data as query parameters
        for (let [key, value] of formData.entries()) {
            url.searchParams.append(key, value);
        }
        const options = {
            method: 'GET',
        };
        showLoader();
        fetch(url, options)
            .then(response => response.json())
            .then(response => {
                console.log(response);
                hideLoader();
                if(response.status && response.status === 'error') {
                    const errorBar = document.getElementById('errorBar');
                    errorBar.textContent = response.message;
                    errorBar.style.display = 'block';
                } else {
                    const successBar = document.getElementById('successBar');
                    successBar.textContent = response.message;
                    successBar.style.display = 'block';
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

    // Function to show the loader
    function showLoader() {
        document.getElementById('loader-overlay').style.display = 'flex';
    }

// Function to hide the loader
    function hideLoader() {
        document.getElementById('loader-overlay').style.display = 'none';
    }

});
