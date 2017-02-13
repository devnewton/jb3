var jb3PasteSketchTemplate = '\
<div name="pasteSketchForm" class="c-fieldset">\
    <div class="o-form-element">\
        <div class="jb3-paste-sketch-tools">\
            <input type="color" value="#000" onchange="{ changeSketchColor }"></input>\
            <input min="0" max="32" value="5" type="range" onchange="{ changeSketchPenSize }"></input>\
            <button class="c-button" onclick="{ undoSketch }">&cularr;</button>\
            <button class="c-button" onclick="{ redoSketch }">&curarr;</button>\
        </div>\
        <div name="sketchCanvasContainer" class="jb3-paste-sketch-container" width="512" height="384"></div>\
    </div>\
    <button class="c-button c-button--info" onclick="{ uploadSketch }" >Upload</button>\
    <progress name="pasteSketchProgress" value="0" max="100"></progress>\
</div>\
<div if="{ pastedSketchUrl }" class="c-card  c-card--success">\
  <div class="c-card__item c-card__item--divider">Pasted!</div>\
  <div class="c-card__item">\
    <p class="c-paragraph"><a class="c-link  jb3-pasted-url" href="{ pastedSketchUrl }" target="_blank">{ pastedSketchUrl }</a></p>\
  </div>\
</div>\
<div if="{ pastedSketchError }" class="c-card  c-card--error">\
  <div class="c-card__item c-card__item--divider">Error :-(</div>\
  <div class="c-card__item">\
    <p class="c-paragraph">{ pastedSketchError }</p>\
  </div>\
</div>\
';
        var jb3PasteSketchStyles = '\
.jb3-paste-sketch-tools {\
    margin-bottom: 10px;\
}\
.jb3-paste-sketch-container {\
    margin:auto;\
    max-width: 100%;\
}\
.jb3-paste-sketch-container canvas {\
    border: 1px solid black;\
    margin:auto;\
    max-width: 100%;\
}\
';
        var jb3PasteSketchConstructor = function () {
        var self = this;
                self.clear = function () {
                self.pastedSketchError = null;
                        self.pastedSketchUrl = null;
                };
                this.sketchpad = new Sketchpad(this.sketchCanvasContainer, {
                width: 512,
                        height: 384,
                        line: {
                        color: '#000',
                                size: 5
                        }
                });
                this.undoSketch = function() {
                this.sketchpad.undo();
                };
                this.redoSketch = function() {
                this.sketchpad.redo();
                };
                this.changeSketchColor = function(e) {
                this.sketchpad.setLineColor(e.target.value);
                };
                this.changeSketchPenSize = function(e) {
                this.sketchpad.setLineSize(e.target.value);
                };
                this.uploadSketch = function() {
                this.sketchpad.canvas.toBlob(function(blob) {
                var formData = new FormData();
                        formData.append("pimage", blob, "sketch.png");
                        var xhr = new XMLHttpRequest;
                        xhr.onprogress = function(e) {
                        var percentComplete = (e.loaded / e.total) * 100;
                                self.pasteSketchProgress.value = percentComplete;
                        };
                        xhr.onreadystatechange = function (event) {
                        if (xhr.readyState == 4) {
                        if (xhr.status == 200) {
                        var data = JSON.parse(xhr.response);
                                self.pastedSketchError = null;
                                self.pastedSketchUrl = data.url;
                        } else {
                        self.pastedSketchError = 'Error during image upload';
                                self.pastedSketchUrl = null;
                        }
                        self.update();
                        }
                        };
                        xhr.open("POST", "/api/paste/image");
                        xhr.send(formData);
                }, "image/png");
                };
        };
        riot.tag('jb3-paste-sketch', jb3PasteSketchTemplate, jb3PasteSketchStyles, jb3PasteSketchConstructor);