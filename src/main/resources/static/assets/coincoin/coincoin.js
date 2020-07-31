const NORLOGE_NORMAL = "HH:mm:ss";
const NORLOGE_FULL = "YYYY-MM-DD HH:mm:ss";

class Jb3 {
    constructor() {
        Handlebars.registerHelper('time2norloge', (time) => {
            return moment(time).format(NORLOGE_NORMAL);
        });
        this.messageTemplate = Handlebars.compile($("#message-template").html());
        this.myLastMessageByRoom = {};
        this.lastBigornoMessageByRoom = {};
        this.lastReplyMessageByRoom = {};
        this.newMessages = [];
        this.controlsMessage = $('#jb3-controls-message');
        this.controlsRoom = $('#jb3-controls-room');
        this.controlsNickname = $('#jb3-controls-nickname');
        this.rooms = {};
        this.rooms[this.controlsRoom.val()] = {};
        jb3_common.getRooms().forEach((room) => {
            this.rooms[room.rname] = {};
        });
        let uri = URI(window.location);
        let roomInURI = uri.search(true).room;
        if (roomInURI) {
        	this.rooms[roomInURI] = {};
        }
        this.controlsRoom.empty().append(
                Object.keys(this.rooms).sort().map((room) => {
            return new Option(room, room);
        })
                );
        let roomInDomain = uri.domain().slice(0, -uri.tld().length - 1);
        roomInDomain = this.rooms[roomInDomain] && roomInDomain;
        this.controlsRoom.attr("size", Math.min(this.controlsRoom.find('option').length, 10));
        this.controlsRoom.val(roomInURI || roomInDomain || localStorage.selectedRoom || this.controlsRoom.find('option').first().val());
        this.controlsMessage.attr("placeholder", this.controlsRoom.val());
        let postsContainer = document.getElementById('jb3-posts-container');
        for (let room in this.rooms) {
            let postsDivForRoom = document.createElement("div");
            postsDivForRoom.dataset.room = room;
            postsDivForRoom.className += "jb3-posts";
            if (room != this.controlsRoom.val()) {
                postsDivForRoom.setAttribute('style', 'display:none')
            }
            postsContainer.appendChild(postsDivForRoom);
            this.rooms[room].postsDiv = postsDivForRoom;
        }

        if (roomInURI === this.controlsRoom.val()) {
            $('#jb3-roster').hide();
            $('header').hide();
            $("#jb3-layout").css('top', '0px');
        }
        this.controlsRoom.change(() => {
        	let previouslySelectedRoom = localStorage.selectedRoom;
            let selectedRoom = localStorage.selectedRoom = this.controlsRoom.val();
            $(`.jb3-posts[data-room!="${selectedRoom}"]`).hide();
            $(`.jb3-posts[data-room="${selectedRoom}"]`).show();
            this.controlsMessage.attr("placeholder", selectedRoom);
            this.scrollPostsContainerToBottom();
            this.trollometre.update(selectedRoom);
            
            localStorage.setItem(`lastReadMessageTime-${previouslySelectedRoom}`, new Date().toISOString());
            this.updateNotifications();
        });
        this.controlsMessage.bind('keydown', (event) => {
            if (event.altKey) {
                if (this.handleAltShortcut(event.key)) {
                    event.stopPropagation();
                    event.preventDefault();
                }
            } else if (event.keyCode === 13) {
            	this.postCurrentMessage();
            }
        });
        if ($('header').css('display') === 'block') {
            $('#jb3-show-controls').html("&slarr;");
        } else {
            $('#jb3-show-controls').html("&equiv;");
        }
        $('#jb3-show-controls').click(() => {
            let header = $('header');
            let layout = $('#jb3-layout');
            let button = $('#jb3-show-controls');
            if (header.css('display') === 'block') {
                header.css('display', 'none');
                layout.css('top', '0px');
                button.html("&equiv;");
            } else {
                header.css('display', 'block');
                layout.css('top', '57px');
                button.html("&slarr;");
            }
            let roster = $('#jb3-roster');
            if (roster.css('display') === 'flex') {
                roster.css('display', 'none');
            } else {
                roster.css('display', 'flex');
            }
        });
        $("#jb3-controls-message-post").click(() => {
            this.postCurrentMessage();
        });
        $("#jb3-controls-message-attach").click(() => {
            this.pasteModal.trigger('show');
        });
        $('.jb3-posts').on('click', '.jb3-post-time', (e) => {
            let postId = $(e.target).parent().attr('id');
            if (postId) {
                this.insertTextWithSpacesAroundInMessageControl('#' + postId);
            }
        });
        $('.jb3-posts').on('click', '.jb3-post-nickname', (e) => {
            let nickname = $(e.target).text();
            if (nickname) {
            	this.insertTextWithSpacesAroundInMessageControl(nickname + '<');
            }
        });
        $('.jb3-posts').on({
            click: (event) => {
                let button = $(event.target);
                let post = button.parents('.jb3-post');
                let revisions = $('#' + post.attr('id') + '-revisions');
                this.revisionsModal.trigger('show', revisions.html());
            }
        }, ".jb3-revisions-button");
        $('.jb3-posts').on({
            click: (event) => {
                let button = $(event.target);
                let post = button.parents('.jb3-post');
                this.insertTextInMessageControl('/revise #' + post.attr('id') + ' ');
            }
        }, ".jb3-post-is-mine .jb3-revise-button");
        $('.jb3-posts').on({
            click: (event) => {
                let spoiler = $(event.target);
                spoiler.toggleClass('jb3-revealed-spoiler');
            }
        }, ".jb3-spoiler");
        $('.jb3-posts').on({
            click: (event) => {
                let button = $(event.target);
                let post = button.parents('.jb3-post');
                this.insertTextInMessageControl('#' + post.attr('id') + ' pan ! pan !');
            }
        }, ".jb3-duck");
        jb3_common.initHighlight();
        jb3_common.initUrlPreview();
        jb3_common.initTotozLazyLoading();
        this.initNickname();
        this.updateMessages();
        this.initTrollometre();
        this.setupGesture();
        this.connect();
        setTimeout(() => this.refreshDlfpToken(), 1000);
        setInterval(() => this.refreshDlfpToken(), 60 * 60 * 1000);
        setTimeout(() => this.sendPresence(), 1000);
        setInterval(() => this.sendPresence(), 60 * 1000);
    }
    
    sendPresence() {
        let data = new FormData();
        data.append("nickname", this.controlsNickname.val());
        data.append("status", "plop");
        fetch("/ssecoin/presence", {
            method: 'POST',  
            body: data
        }
        );
    }
    
    connect() {
        let sseCoin = new EventSource(`/ssecoin/posts/stream?rooms=${Object.keys(this.rooms).join(',')}`);
        sseCoin.onmessage = (event) => {
            this.newMessages = this.newMessages.concat(JSON.parse(event.data));
        };
        sseCoin.addEventListener("presence", (event) => {
            this.moulesRoster.trigger('presence', JSON.parse(event.data));
        });
        sseCoin.onerror = (err) => {
            console.log(`Lost connection, retry in 10 seconds`);
            sseCoin.close();
            setTimeout(() => this.connect(), 10000);
        };      
    }
    
    setupGesture() {
        delete Hammer.defaults.cssProps.userSelect;
        let hammertime = new Hammer(document.getElementById("jb3-posts-container"), {
            inputClass: Hammer.TouchInput
        });
        hammertime.on('swipeleft', (e) =>{
        	let tribuneSelect = this.controlsRoom[0];
            if (tribuneSelect.selectedIndex === 0) {
                tribuneSelect.selectedIndex = tribuneSelect.options.length - 1;
            } else {
                tribuneSelect.selectedIndex = tribuneSelect.selectedIndex - 1;
            }
            this.controlsRoom.change();
        }
        );
        hammertime.on('swiperight', (e) => {
        	let tribuneSelect = this.controlsRoom[0];
            if (tribuneSelect.selectedIndex >= (tribuneSelect.options.length - 1)) {
                tribuneSelect.selectedIndex = 0;
            } else {
                tribuneSelect.selectedIndex = tribuneSelect.selectedIndex + 1;
            }
            this.controlsRoom.change();
        });
    }
    
    postCurrentMessage() {
        let selectedRoom = this.controlsRoom.val();
        let auth = this.checkAuth(selectedRoom);
        if (auth) {
            let data = new FormData();
            data.append("message", this.controlsMessage.val());
            data.append("nickname", this.controlsNickname.val());
            data.append("room", selectedRoom);
            data.append("auth", auth);
            fetch("/ssecoin/posts/add", {
                method: 'POST',  
                body: data
            }
            ).then(response => {
                if(response.ok) {
                    this.controlsMessage.val('');
                }
            } );
            
        }
    }
    
    checkAuth(selectedRoom) {
        if (selectedRoom === 'dlfp') {
            let auth = localStorage.getItem("dlfp-auth");
            if (this.checkIfDlfpTokenIsExpired(auth)) {
                window.location.href = "/dlfp/connect";
                return false;
            } else {
                return auth;
            }
        } else if(selectedRoom === 'gamatomic') {
            let auth =  localStorage.getItem('gamatomic-auth');
            if(auth) {
                return auth;
            } else {
                window.alert("Please configure gamatomic cookie (in rooms page)");
                return false;
            }
        } else {
            return true;
        }
    }
    
    checkIfDlfpTokenIsExpired(authStr) {
        if (!authStr) {
            return true;
        }
        let auth = JSON.parse(authStr);
        if (!auth.expires_timestamp) {
            return true;
        }
        return auth.expires_timestamp < Date.now();
    }
    
    updateMessages() {
    	this.onNewMessages(this.newMessages.splice(0, 500));
        setTimeout(() => {
        	this.updateMessages();
        }, 1000);
    }
    
    initNickname() {
    	this.controlsNickname.val(jb3_common.getNickname());
    	this.controlsNickname.change(() =>{
            jb3_common.setNickname(this.controlsNickname.val());
        });
        riot.mount('jb3-raw');
        riot.mount('jb3-modal');
        this.moulesRoster = riot.mount('jb3-moules-roster')[0];
        this.revisionsModal = riot.mount('jb3-revisions-modal')[0];
        this.pasteModal = riot.mount('jb3-paste-modal')[0];
        this.pasteModal.on('pasted', (pastedText) => {
        	this.insertTextInMessageControl(pastedText);
        });
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

    isPostsContainerAtBottom() {
        let postContainer = $('#jb3-posts-container');
        return Math.ceil(postContainer.scrollTop() + postContainer.innerHeight()) >= postContainer[0].scrollHeight;
    }
    
    scrollPostsContainerToBottom() {
        let postContainer = $('#jb3-posts-container');
        postContainer.scrollTop(postContainer.prop("scrollHeight"));
    }
    
    onNewMessages(data) {
        if (data && data.length > 0) {
            let userNickname = $('#jb3-controls-nickname').val();
            let wasAtbottom = this.isPostsContainerAtBottom();
            for (let d in data) {
                let message = data[d];
                this.trollometre.feed(message);
                this.onMessage(userNickname, message);
            }
            this.updateNorloges();
            this.updateNotifications();
            this.trollometre.update(this.controlsRoom.val());
            if (wasAtbottom) {
            	this.scrollPostsContainerToBottom();
            }
        }
    }
    
    onMessage(userNickname, message) {
        message.message = jb3_post_to_html.parse(message.message);
        message.postIsMine = message.nickname === userNickname || (message.room && message.nickname === localStorage.getItem(message.room + '-login')) ? " jb3-post-is-mine" : "";
        message.postIsBigorno = message.message.search(new RegExp("(moules|" + RegExp.escape(userNickname) + ")&lt;", "i")) >= 0 ? " jb3-post-is-bigorno" : "";
        if(message.postIsMine) {
        	let lastMessage = this.myLastMessageByRoom[message.room];
        	if(!lastMessage || lastMessage.time < message.time) {
        		this.myLastMessageByRoom[message.room] = message;
        	}
        }
        if(message.postIsBigorno) {
        	let lastBigorno = this.lastBigornoMessageByRoom[message.room];
        	if(!lastBigorno || lastBigorno.time < message.time) {
        		this.lastBigornoMessageByRoom[message.room] = message;
        	}
        }
        let messageDiv = this.messageTemplate(message);
        this.insertMessageDiv(messageDiv, message);
    }
    
    insertMessageDiv(messageDiv, message) {
        let existingDiv = document.getElementById(message.id);
        if (!existingDiv) {
        	let room = this.rooms[message.room];
        	if(room && room.postsDiv) {
	            let container = room.postsDiv;
	            let dates = container.getElementsByClassName("jb3-posts-date");
	            let day = moment(message.time);
	            let date = null;
	            let dateAfter = null;
	            for (let d = 0; d < dates.length; ++d) {
	                let dateDay = moment(dates[d].dataset.date);
	                if (dateDay.isSame(day, 'day')) {
	                    date = dates[d];
	                    break;
	                }
	                if (dateDay.isAfter(day, 'day')) {
	                    dateAfter = dates[d];
	                    break;
	                }
	            }
	            if (!date) {
	                date = document.createElement('div');
	                date.classList.add("jb3-posts-date");
	                date.dataset.date = day.format("YYYY-MM-DD");
	                let dateTitle = document.createElement('time');
	                dateTitle.appendChild(document.createTextNode(day.format("dddd D MMMM YYYY")));
	                date.appendChild(dateTitle);
	                if (dateAfter) {
	                    dateAfter.insertAdjacentElement('beforebegin', date);
	                } else {
	                    container.insertAdjacentElement('beforeend', date);
	                }
	            }
	            let t = message.time;
	            let posts = date.getElementsByClassName('jb3-post');
	            for (let p = 0; p < posts.length; ++p) {
	                let post = posts[p];
	                if (t < post.dataset.time) {
	                    post.insertAdjacentHTML('beforebegin', messageDiv);
	                    return;
	                }
	            }
	            date.insertAdjacentHTML('beforeend', messageDiv);
        	}
        } else {
            existingDiv.outerHTML = messageDiv;
        }

    }
    
    updateNorloges() {
        $('.jb3-cite-raw').each((_, element) => {
            let cite = $(element);
            let postId = cite.data('ref');
            let cited = $('#' + postId);
            let citedNorloge = cited.find('.jb3-post-time');
            if (citedNorloge.length > 0) {
                cite.text(citedNorloge.text());
                cite.removeClass('jb3-cite-raw');
            } else {
                fetch(`/ssecoin/posts/find/${postId}`).then(response => response.ok && response.json() ).then((post) => {
                    $(`.jb3-cite-raw[data-ref='${post.id}']`).each((_, element) =>{
                        let cite = $(element);
                        cite.text(moment(post.time).format(NORLOGE_FULL));
                        cite.removeClass('jb3-cite-raw');
                    });
                });
            }
            if (cited.hasClass('jb3-post-is-mine')) {
                cited.addClass('jb3-cited-by-me');
                cite.addClass('jb3-cite-mine');
                let post = cite.closest('.jb3-post');
                post.addClass('jb3-post-is-reply-to-mine');
                
                let reply = {message: post.text(), id: post.attr("id"), room: post.attr("data-room"), time: post.attr("data-time")};
                let lastReply = this.lastReplyMessageByRoom[reply.room];
            	if(!lastReply || lastReply.time < reply.time) {
            		this.lastReplyMessageByRoom[reply.room] = reply;
            	}
            } else {
                cited.addClass('jb3-cited');
            }
        });
    }
    
    updateNotifications() {
    	let bigorno = false;
    	let reply = false;
    	for (let room in this.rooms) {
    		bigorno |= this.updateNotification(room, "\uD83D\uDCE3", this.lastBigornoMessageByRoom);
    		reply |= this.updateNotification(room, "\u21AA", this.lastReplyMessageByRoom);
    	}
    	if(bigorno && document.title.indexOf("\uD83D\uDCE3") < 0) {
    		document.title = `\uD83D\uDCE3${document.title}`;
    	}
    	if(reply && document.title.indexOf("\u21AA") < 0) {
    		document.title = `\u21AA${document.title}`;
    	}
    }
    
    momentOfMessage(message) {
    	const JB3_OLDEST_MOMENT = moment("2000-01-01 12:00:00Z");
    	return message && message.time && moment(message.time) || JB3_OLDEST_MOMENT;
    }
    
    lastReadMessageMoment(room) {
    	const JB3_OLDEST_MOMENT = moment("2000-01-01 12:00:00Z");
    	let str = localStorage.getItem(`lastReadMessageTime-${room}`);
    	if(str) {
    		return moment(str);
    	} else {
    		return JB3_OLDEST_MOMENT;
    	}
    }
       
    updateNotification(room, notificationString, lastNotifierMessages) {
		let notifier = lastNotifierMessages[room];
		let notifierMoment = this.momentOfMessage(notifier);
		if(notifierMoment.isAfter(this.lastReadMessageMoment(room)) && notifierMoment.isAfter(this.momentOfMessage(this.myLastMessageByRoom[room]))) {
			this.addNotification(notifier, notificationString);
			return true;
		} else {
			this.clearNotification(room, notifier, notificationString);
			return false;
		}		
    }
    
    addNotification(message, notificationString) {
    	let room = document.querySelector(`#jb3-controls-room option[value="${message.room}"`);
    	if(room && room.innerText.indexOf(notificationString) < 0) {
    		room.innerText += notificationString;
    	}
    }
    
    clearNotification(roomName, message, notificationString) {
    	document.title = document.title.replace(notificationString, "");
    	let room = document.querySelector(`#jb3-controls-room option[value="${roomName}"`);
    	if(room) {
    		room.innerText = room.innerText.replace(notificationString, "");
    	}
    }
    
    handleAltShortcut(keychar) {
        switch (keychar) {
            case 'o':
                this.insertTextInMessageControl('_o/* <b>BLAM</b>! ');
                return true;
            case 'm':
                this.insertTextInMessageControl('====> <b>Moment ' + this.getSelectedText() + '</b> <====', 16);
                return true;
            case 'f':
                this.insertTextInMessageControl('\u03C6');
                return true;
            case 'b':
                this.insertTextInMessageControl('<b>' + this.getSelectedText() + '</b>', 3);
                return true;
            case 'i':
                this.insertTextInMessageControl('<i>' + this.getSelectedText() + '</i>', 3);
                return true;
            case 'u':
                this.insertTextInMessageControl('<u>' + this.getSelectedText() + '</u>', 3);
                return true;
            case 's':
                this.insertTextInMessageControl('<s>' + this.getSelectedText() + '</s>', 3);
                return true;
            case 't':
                this.insertTextInMessageControl('<tt>' + this.getSelectedText() + '</tt>', 4);
                return true;
            case 'c':
                this.insertTextInMessageControl('<code>' + this.getSelectedText() + '</code>', 6);
                return true;
            case 'd':
                this.insertTextInMessageControl('<spoiler>' + this.getSelectedText() + '</spoiler>', 9);
                return true;
            case 'p':
                this.insertTextInMessageControl('_o/* <b>paf!</b> ');
                return true;
            case 'a':
                this.insertTextInMessageControl('\u266A <i>' + this.getSelectedText() + '</i> \u266A', 5);
                return true;
        }
        return false;
    }
    
    getSelectedText() {
        let controlsMessage = document.getElementById("jb3-controls-message");
        if (controlsMessage) {
            return controlsMessage.value.substring(controlsMessage.selectionStart, controlsMessage.selectionEnd);
        } else {
            return"";
        }
    }
    
    insertTextInMessageControl(text, pos) {
        let control = document.getElementById("jb3-controls-message");
        if (!pos) {
            pos = text.length;
        }
        let selectionEnd = control.selectionStart + pos;
        control.value = control.value.substring(0, control.selectionStart) + text + control.value.substr(control.selectionEnd);
        control.focus();
        control.setSelectionRange(selectionEnd, selectionEnd);
    }
    
    insertTextWithSpacesAroundInMessageControl(text) {
        let control = document.getElementById("jb3-controls-message");
        let textBefore = control.value.substring(0, control.selectionStart);
        if (/.*\S$/.test(textBefore)) {
            textBefore = textBefore.concat(" ");
        }
        let textAfter = control.value.substr(control.selectionStart);
        let firstPart = textBefore.concat(text).concat(' ');
        let caretPos = firstPart.length;
        control.value = firstPart.concat(textAfter);
        control.focus();
        control.setSelectionRange(caretPos, caretPos);
    }
    
    initTrollometre() {
        this.trollometre = new Trollometre(document.getElementById("trollometre"));
    }
    
    refreshDlfpToken() {
        let dlfpAuth = localStorage.getItem("dlfp-auth");
        if (dlfpAuth) {
            let xhr = new XMLHttpRequest();
            xhr.onreadystatechange = (event) => {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        localStorage.setItem("dlfp-auth", xhr.response);
                    }
                }
            };
            let token = JSON.parse(dlfpAuth);
            let body = new FormData();
            body.append("token", token.refresh_token);
            xhr.open("POST", "/dlfp/refresh-token");
            xhr.send(body);
        }
    }
}

let jb3 = new Jb3();
