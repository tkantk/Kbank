import jQuery from "jquery";
jQuery(function($) {
    "use strict";
    $(document).ready(function() {
        showLoader();
        // Make an AJAX call to your servlet
        $.ajax({
            url: '/content/wbank/us/en/servlets-content-folder/send-personalized-email/jcr:content.json?type=articlelist', // Replace with your actual servlet URL
            method: 'GET',
            dataType: 'json',
            success: function(data) {
                var $blogContainer = $('#blogContainer'); // Replace with your container's ID

                // Iterate through the data and create blog cards
                $.each(data.data, function(index, item) {
                    var truncatedDescription = truncateText(item.article, 50);
                    var blogCard = `
                        <div class="blog-card">
                            <div class="card-img">
                                <img src="${item.urlToImage}" alt="${item.heading}">
                                <h1>${item.heading}</h1>
                            </div>
                            <div class="card-details"></div>
                            <div class="card-text"><p>${truncatedDescription}</p></div>
                            <div class="read-more">
                                <a href="#" target="_blank">Read More</a>
                            </div>
                            <div id="articlePopup${index}" class="popup">
                                <div class="popup-content">
                                    <button class="close-btn popup-close" data-popup-id="articlePopup${index}">&times;</button>
                                    <h2 class="popup-title">${item.heading}</h2>
                                    <img src="${item.urlToImage}" alt="Article Image" class="popup-image">
                                    <div class="popup-description">
                                        ${item.article}
                                    </div>
                                    <a href="${item.url}" class="popup-link">Check this story at the source</a>
                                </div>
                            </div>
                        </div>
                    `;

                    $blogContainer.append(blogCard);
                });

                let timeout;
                $('.blog-card').on('mouseenter mouseleave', function(e) {
                    clearTimeout(timeout);
                    const $card = $(this);
                    timeout = setTimeout(function() {
                        if (e.type === 'mouseenter') {
                            $card.addClass('hovered');
                        } else {
                            $card.removeClass('hovered');
                        }
                    }, 50); // Adjust this delay as needed
                });

                // Event delegation for closing popups
                $(document).on('click', '.close-btn, .popup', function(e) {
                    if (e.target === this) {
                        closePopup();
                    }
                });

                // Add event listener for "Read More" links to show popups
                $('.read-more a').on('click', function(e) {
                    e.preventDefault();
                    var popupId = $(this).closest('.blog-card').find('.popup').attr('id');
                    openPopup(popupId);
                });
                hideLoader();
            },
            error: function(xhr, status, error) {
                console.error("Error fetching blog data:", error);
                $('#blogContainer').html('<p>Error loading blog posts. Please try again later.</p>');
                hideLoader();
            }
        });

    });

    // Function to open popup
    function openPopup(popupId) {
        var $popup = $('#' + popupId);
        $popup.css('display', 'block');
        // Trigger reflow
        $popup[0].offsetHeight;
        $popup.addClass('active');
        $('body').addClass('popup-open');
    }

    // Function to close popup
    function closePopup() {
        var $popup = $('.popup.active');
        $popup.removeClass('active');
        setTimeout(function() {
            $popup.css('display', 'none');
            $('body').removeClass('popup-open');
        }, 300); // Match this to your CSS transition time
    }

    // Close popup on ESC key press
    $(document).keydown(function(e) {
        if (e.keyCode === 27) { // ESC key
            closePopup();
        }
    });

    // Function to truncate text
    function truncateText(text, maxLength) {
        if (text.length > maxLength) {
            return text.substring(0, maxLength) + '...';
        }
        return text;
    }

    // Function to show the loader
    function showLoader() {
        const loaderOverlay = document.getElementById('loader-overlay');
        if (loaderOverlay) {
            loaderOverlay.style.display = 'flex';
            // Optional: Add a small delay before setting opacity to 1 for a smooth fade-in effect
            setTimeout(() => {
                loaderOverlay.style.opacity = '1';
            }, 10);
        } else {
            console.error('Loader overlay element not found');
        }
    }

// Function to hide the loader
    function hideLoader() {
        const loaderOverlay = document.getElementById('loader-overlay');
        if (loaderOverlay) {
            loaderOverlay.style.opacity = '0';
            // Wait for the fade-out transition to complete before setting display to 'none'
            setTimeout(() => {
                loaderOverlay.style.display = 'none';
            }, 300); // Adjust this value to match your transition duration
        } else {
            console.error('Loader overlay element not found');
        }
    }
});
