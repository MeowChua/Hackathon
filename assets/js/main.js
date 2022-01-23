const video = document.getElementById("video");
var react = document.getElementsByClassName("feel");
let predictedAges = [];
console.log('../models')
Promise.all([
  
  faceapi.nets.tinyFaceDetector.loadFromUri("../assets/models"),
  faceapi.nets.faceLandmark68Net.loadFromUri("../assets/models"),
  faceapi.nets.faceRecognitionNet.loadFromUri("../assets/models"),
  faceapi.nets.faceExpressionNet.loadFromUri("../assets/models"),
  faceapi.nets.ageGenderNet.loadFromUri("../assets/models")
]).then(startVideo);

function startVideo() {
  navigator.getUserMedia(
    { video: {} },
    stream => (video.srcObject = stream),
    err => console.error(err)
  );
}

video.addEventListener("playing", () => {
  const canvas = faceapi.createCanvasFromMedia(video);

  const displaySize = { width: video.width, height: video.height };
  faceapi.matchDimensions(canvas, displaySize);

  setInterval(async () => {
    const detections = await faceapi
      .detectAllFaces(video, new faceapi.TinyFaceDetectorOptions())
      .withFaceLandmarks()
      .withFaceExpressions()
      .withAgeAndGender();
    const resizedDetections = faceapi.resizeResults(detections, displaySize);

    // canvas.getContext("2d").clearRect(0, 0, canvas.width, canvas.height);
    
    var max = 0;
    var reaction = ['angry', 'happy', 'neutral', 'sad', 'surprised'];
    var bestReaction = '';
    if(resizedDetections.length > 0) {
      for(var i of reaction) {
        if(max < resizedDetections[0].expressions[i]) {
            max = resizedDetections[0].expressions[i];
            bestReaction = i;
        }
      }
      var text = bestReaction;
      for(var i=0;i<react.length;i++){
        react[i].innerHTML = text;
      }
      if(text == "happy"){
        for(var i=0;i<react.length;i++){
          react[i].style.color = "green";
        }
      }
      if(text == "neutral"){
        for(var i=0;i<react.length;i++){
          react[i].style.color = "white";
        }
      }
      if(text == "surprised"){
        for(var i=0;i<react.length;i++){
          react[i].style.color = "orange";
        }
      }
      if(text == "angry"){
        for(var i=0;i<react.length;i++){
          react[i].style.color = "red";
        }
      }
      if(text == "sad"){
        for(var i=0;i<react.length;i++){
          react[i].style.color = "violet";
        }
      }}
  }, 100);
});
