
var jb3PasteSketchConstructor = function () {
    var self = this;
    self.clear = function () {
        self.sketchpad.clear();
        self.pastedSketchError = null;
        self.pasted = null;
    };
    this.sketchpad = new Sketchpad(this.sketchCanvasContainer, {
        width: 512,
        height: 384,
        aspectRatio: 512/384,
        backgroundColor: '#FFFFFF',
        line: {
            color: '#000',
            size: 5
        }
    });
    document.addEventListener('paste', function (event) {
        if(!( self.pasteSketchForm.offsetWidth || self.pasteSketchForm.offsetHeight || self.pasteSketchForm.getClientRects().length ) ) {
           return; 
        }
        var items = (event.clipboardData || event.originalEvent.clipboardData).items || [];
        for (var i = 0; i < items.length; i++) {
            if (items[i].type.indexOf("image") === 0) {
                var file = items[i].getAsFile();
                if (file) {
                    var img = new Image();
                    img.addEventListener('load', function () {
                        self.sketchpad.setBackgroundImage(img);
                        self.sketchpad.resize();
                        self.update();
                        self.sketchSize.value = 'original';
                    });
                    img.src = window.URL.createObjectURL(file);
                }
            }
        }
        event.preventDefault();
    });
    this.pasteBackground = function () {
        document.execCommand('paste');
    };
    this.undoSketch = function () {
        this.sketchpad.undo();
    };
    this.redoSketch = function () {
        this.sketchpad.redo();
    };
    this.changeSketchColor = function (e) {
        this.sketchpad.setLineColor(e.target.value);
    };
    this.changeSketchPenSize = function (e) {
        this.sketchpad.setLineSize(e.target.value);
    };
    this.changeSketchSize = function(e) {
        switch(e.target.value) {
            case 'small':
                self.sketchpad.resize(512);
                break;
            case 'big':
                self.sketchpad.resize(1024);
                break;
            case 'original':
                self.sketchpad.resize();
                break;
            case 'fit':
                self.sketchpad.resize(self.sketchCanvasContainer.offsetWidth);
                break;
        }
    };
    this.uploadSketch = function () {
        this.sketchpad.canvas.toBlob(function (blob) {
            if(self.pasteSketchProgress.scrollIntoView) {
                self.pasteSketchProgress.scrollIntoView();
            }
            var formData = new FormData();
            formData.append("pimage", blob, "sketch.jpg");
            var xhr = new XMLHttpRequest;
            xhr.upload.onprogress = function (e) {
                if(e.lengthComputable) {
                    var percentComplete = (e.loaded / e.total) * 100;
                    self.pasteSketchProgress.value = percentComplete;
                }
            };
            xhr.onreadystatechange = function (event) {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        var data = JSON.parse(xhr.response);
                        self.pastedSketchError = null;
                        self.pasted = data.url;
                    } else {
                        self.pastedSketchError = 'Error during image upload';
                        self.pasted = null;
                    }
                    self.trigger('paste-content-changed');
                    self.update();
                    if (self.pastedResult && self.pastedResult.scrollIntoView) {
                        self.pastedResult.scrollIntoView();
                    }
                }
            };
            xhr.open("POST", "/api/paste/image");
            xhr.send(formData);
        }, "image/jpeg", 0.9);
    };
};

var jb3PasteSketchStyles = '\
.jb3-paste-sketch-tools {\
    margin-bottom: 10px;\
}\
.jb3-paste-sketch-tools .c-field {\
    display: inline;\
    width: auto;\
}\
.jb3-paste-sketch-container {\
    margin:auto;\
    overflow:auto\
}\
.jb3-paste-sketch-container canvas {\
    border: 1px solid black;\
    margin:auto;\
}\
';

var jb3PasteSketchTemplate = '\
<div name="pasteSketchForm" class="c-fieldset">\
    <div class="o-form-element">\
        <div class="jb3-paste-sketch-tools">\
            <input type="color" value="#000" onchange="{ changeSketchColor }"></input>\
            <input min="0" max="32" value="5" type="range" onchange="{ changeSketchPenSize }"></input>\
            <button class="c-button" onclick="{ undoSketch }">&cularr;</button>\
            <button class="c-button" onclick="{ redoSketch }">&curarr;</button>\
            <select name="sketchSize" class="c-field" onchange="{ changeSketchSize }">\
                <option value="small">small</option>\
                <option value="big">big</option>\
                <option value="original" if="{ sketchpad.getBackgroundImage() }">original</option>\
                <option value="fit">fit</option>\
            </select>\
            <button class="c-button" onclick="{ clear }">Clear</button>\
            <button class="c-button c-button--info" onclick="{ uploadSketch }" >Upload</button>\
        </div>\
        <div name="sketchCanvasContainer" class="jb3-paste-sketch-container" width="512" height="384"></div>\
    </div>\
    <progress name="pasteSketchProgress" value="0" max="100"></progress>\
</div>\
<div name="pastedResult">\
    <div if="{ pasted }" class="c-card">\
      <div class="c-card__item c-card__item--divider c-card__item--success">Pasted!</div>\
      <div class="c-card__item">\
        <p class="c-paragraph"><a class="c-link  jb3-pasted-url" href="{ pasted }" target="_blank">{ pasted }</a></p>\
      </div>\
    </div>\
    <div if="{ pastedSketchError }" class="c-card">\
      <div class="c-card__item c-card__item--divider c-card__item--error">Error :-(</div>\
      <div class="c-card__item">\
        <p class="c-paragraph">{ pastedSketchError }</p>\
      </div>\
    </div>\
</div>\
';
riot.tag('jb3-paste-sketch', jb3PasteSketchTemplate, jb3PasteSketchStyles, jb3PasteSketchConstructor);