class Jb3Common {
    getNickname() {
        return localStorage.nickname || (localStorage.nickname = this.randomNickname());
    }

    setNickname(nickname) {
        localStorage.nickname = nickname;
    }

    randomNickname() {
        let letters = ["aeiouy", 'bcdfghjklmnpqrstvwxz'];
        let nicknameLength = 3 + Math.floor(Math.random() * 5);
        let lettersIndex = Math.floor(Math.random() * letters.length);
        let nickname = "";
        for (let l = 0; l < nicknameLength; ++l) {
            let c = letters[lettersIndex].charAt(Math.floor(Math.random() * letters[lettersIndex].length));
            nickname = nickname.concat(c);
            lettersIndex = (lettersIndex + 1) % letters.length;
        }
        return nickname;
    }

    getRooms() {
        let rooms;
        try {
            rooms = JSON.parse(localStorage.rooms);
        } catch (e) {
        }
        if (!rooms) {
            rooms = this.getDefaultRooms();
        }
        return rooms;
    }

    getDefaultRooms() {
        return document.getElementById('jb3-defaults').dataset.rooms.split(",").map((room) => {
            return {rname: room};
        });
    }

    initTotozLazyLoading() {
        $('.jb3-posts').on({
            mouseenter: (event) => {
                let totoz = $(event.target);
                if (totoz.find('img').length === 0) {
                    let totozImg = '<img src="/totoz/img/' + encodeURI(totoz.text()) + '"/>';
                    totoz.append(totozImg);
                }
            },
            mouseleave: (event) => {
            }
        }, ".jb3-totoz");
    }

    initUrlPreview() {
        $('.jb3-posts').on({
            mouseenter: (event) => {
                let url = $(event.currentTarget);
                if (url.find('.jb3-url-preview').length === 0) {
                    let xhr = new XMLHttpRequest();
                    xhr.onreadystatechange = (event) => {
                        if (xhr.readyState === 4) {
                            if (xhr.status === 200) {
                                if (url.find('.jb3-url-preview').length === 0) {
                                    let preview = JSON.parse(xhr.response);
                                    let previewFigure = `<figure class="jb3-url-preview">${preview.image ? `<img src="${preview.image}" />` : ``}<figcaption><h4>${preview.title}</h4>${preview.description}</figcaption></figure>`;
                                    url.append(previewFigure);
                                }
                            }
                        }
                    };
                    xhr.open("GET", "/api/preview?url=" + encodeURIComponent(url.attr('href')));
                    xhr.send();
                }
            },
            mouseleave: (event) => {
            }
        }, "a");
    }

    initHighlight() {
        $('.jb3-posts').on({
            mouseenter: (event) => {
                this.highlightPostAndReplies($(event.target).data('ref'), true);
            },
            mouseleave: (event) => {
                this.unhighlightPostAndReplies($(event.target).data('ref'));
            },
            click: (event) => {
                let postContainer = $('#jb3-posts-container');
                let postId = $(event.target).data('ref');
                let quoted = $('#' + postId);
                if (quoted.length > 0) {
                    postContainer.scrollTop(quoted[0].offsetTop - event.clientY + postContainer.offset().top + 10);
                } else {
                    window.open("/archives/post/" + postId, "_blank");
                }
            }
        }, ".jb3-cite");
        $('.jb3-posts').on({
            mouseenter: (event) => {
                this.highlightPostAndReplies($(event.target).parent().attr('id'), false);
            },
            mouseleave: (event) => {
                this.unhighlightPostAndReplies($(event.target).parent().attr('id'));
            }
        }, ".jb3-post-time");
    }

    highlightPostAndReplies(postId, showPopup) {
        let post = $('#' + postId);
        post.addClass("jb3-highlight");
        if (showPopup) {
            $('#jb3-post-popup-content').html(post.html());
        }
        $(".jb3-cite[data-ref='" + post.attr('id') + "']").addClass("jb3-highlight");
    }

    unhighlightPostAndReplies(postId) {
        let post = $('#' + postId);
        post.removeClass("jb3-highlight");
        $(".jb3-cite[data-ref='" + post.attr('id') + "']").removeClass("jb3-highlight");
        $('#jb3-post-popup-content').empty();
    }

    async withTimeout(ms, f) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), ms);
        try {
            return await f(controller.signal);
        } finally {
            clearTimeout(timeoutId);
        }
    }
}
;

jb3_common = new Jb3Common();

RegExp.escape = (str) => {
    return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
};