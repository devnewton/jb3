riot
        .tag(
                'jb3-paste-form',
                '\
		<div class="c-tabs">\
		    <div class="c-tabs__headings">\
			    <div class="c-tab-heading { \'c-tab-heading--active\': selectedTab == \'text\' }" data-tab="text" onclick="{ selectTab }">Text</div>\
			    <div class="c-tab-heading { \'c-tab-heading--active\': selectedTab == \'image\' }" data-tab="image" onclick="{ selectTab }">Image</div>\
			    <div class="c-tab-heading { \'c-tab-heading--active\': selectedTab == \'sketch\' }" data-tab="sketch" onclick="{ selectTab }">Sketch</div>\
                            <div class="c-tab-heading { \'c-tab-heading--active\': selectedTab == \'record\' }" data-tab="record" onclick="{ selectTab }">Record</div>\
			    <div class="c-tab-heading { \'c-tab-heading--active\': selectedTab == \'file\' }" data-tab="file" onclick="{ selectTab }">File</div>\
		     </div>\
			<div class="c-tabs__tab { \'c-tabs__tab--active\': selectedTab == \'text\' }">\
                            <jb3-paste-text></jb3-paste-text>\
			</div>\
			<div class="c-tabs__tab { \'c-tabs__tab--active\': selectedTab == \'image\' }">\
				<jb3-paste-image></jb3-paste-image>\
			</div>\
                        <div class="c-tabs__tab { \'c-tabs__tab--active\': selectedTab == \'sketch\' }">\
                            <jb3-paste-sketch></jb3-paste-sketch>\
			</div>\
                        <div class="c-tabs__tab { \'c-tabs__tab--active\': selectedTab == \'record\' }">\
                            <div class="o-form-element">\
                                <button class="c-button" onclick="{ toggleMic }">Switch { mediaRecorder ? "off" : "on" } mic</button>\
                                <button if="{ mediaRecorder }" class="c-button" onclick="{ toggleRecord }">\
                                    { mediaRecorder && mediaRecorder.state == "recording" ? "Stop" : "Record" }\
                                    <img style="width:1em; height:1em;" if="{ mediaRecorder && mediaRecorder.state == \'recording\'}" src="/assets/icons/audio.svg" alt="recording">\
                                </button>\
                            </div>\
                            <div class="o-form-element">\
                                <audio name="recordedAudio" controls></audio>\
                            </div>\
                            <div class="o-form-element">\
                                <button class="c-button c-button--info" onclick="{ uploadRecord }" >Upload</button>\
                                <progress name="recordProgress" value="0" max="100"></progress>\
                            </div>\
                            <div if="{ pastedRecordUrl }" class="c-card  c-card--success">\
				  <div class="c-card__item c-card__item--divider">Pasted!</div>\
				  <div class="c-card__item">\
				    <p class="c-paragraph"><a class="c-link  jb3-pasted-url" href="{ pastedRecordUrl }">{ pastedRecordUrl }</a></p>\
				  </div>\
                            </div>\
                            <div if="{ pastedRecordError }" class="c-card  c-card--error">\
                              <div class="c-card__item c-card__item--divider">Error :-(</div>\
                              <div class="c-card__item">\
                                <p class="c-paragraph">{ pastedRecordError }</p>\
                              </div>\
                            </div>\
                        </div>\
			<div class="c-tabs__tab { \'c-tabs__tab--active\': selectedTab == \'file\' }">\
                            <jb3-paste-file></jb3-paste-file>\
			</div>\
		</div>\
',
               '',
                function(opts) {
                    var self = this;
                    self.pasteText = self.tags['jb3-paste-text'];
                    self.pasteFile = self.tags['jb3-paste-file'];
                    self.pasteImage = self.tags['jb3-paste-image'];
                    self.pasteSketch = self.tags['jb3-paste-sketch'];
                    this.selectedTab = 'text';
                        this.toggleMic = function() {
                            if(self.mediaRecorder) {
                                var mediaRecorder = self.mediaRecorder;
                                self.mediaRecorder = null;
                                mediaRecorder.stream.getTracks().forEach(function(track) {
                                    track.stop();
                                });
                            } else {
                                var constraints = { audio: true, video: false }; 
                                navigator.mediaDevices.getUserMedia(constraints).then(function(mediaStream) {
                                    self.mediaRecorder = new MediaRecorder(mediaStream);
                                    self.recordedChunks = [];
                                    self.mediaRecorder.ondataavailable = function(e) {
                                        self.recordedChunks.push(e.data);
                                    }
                                    self.mediaRecorder.onstop = function() {
                                        self.recordedBlob = new Blob(self.recordedChunks, { 'type' : 'audio/ogg; codecs=opus' });
                                        self.recordedChunks = [];
                                        self.recordedAudio.src = window.URL.createObjectURL(self.recordedBlob);
                                        self.recordedAudio.play();
                                    };
                                    self.update();
                                },function(){
                                    console.log('No audio recording device available.');
                                })
                                .catch(function(err) { 
                                    console.log(err.name + ": " + err.message);
                                });
                            }
                        };
                        this.toggleRecord = function() {
                            if(self.mediaRecorder && self.mediaRecorder.state == "recording") {
                                self.mediaRecorder.stop();
                            } else {
                                self.recordedChunks = [];
                                self.mediaRecorder.start();
                            }
                        };
                        this.stopRecord = function() {
                            if(self.mediaRecorder) {
                                this.mediaRecorder.onstop = function() {

                                }
                                self.mediaRecorder.stop();
                            }
                        };
                        this.uploadRecord = function() {
                            if(self.recordedBlob) {
                                var formData = new FormData();
                                formData.append("pfile", self.recordedBlob, "record.ogg");
                                var xhr = new XMLHttpRequest;
                                xhr.onprogress = function(e) {
                                    var percentComplete = (e.loaded / e.total) * 100;
                                    self.recordProgress.value = percentComplete;
                                };
                                xhr.onreadystatechange = function (event) {
                                if (xhr.readyState == 4) {
                                if (xhr.status == 200) {
                                var data = JSON.parse(xhr.response);
                                        self.pastedRecordError = null;
                                        self.pastedRecordUrl = data.url;
                                } else {
                                self.pastedRecordError = 'Error during record upload';
                                        self.pastedRecordUrl = null;
                                }
                                self.update();
                                }
                                };
                                xhr.open("POST", "/api/paste/file");
                                xhr.send(formData);
                            }
                        };
                        self.selectTab = function(e) {
                        self.selectedTab = e.target.dataset.tab
                        };
                        self.clear = function() {
                            self.pasteText.clear();
                            self.pasteFile.clear();
                            self.pasteImage.clear();
                            self.pasteSketch.clear();
                            self.pastedRecordUrl = null;
                            self.pastedRecordError = null;
                            this.update();
                        }
                });