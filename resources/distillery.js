function video_jump(pos) {
  var player = $("#main_video")[0];
  if (player.paused) {
    player.play();
  }
  player.currentTime = pos;
}

function innerpage(id) {
  $("article.innerpage").css("display", "none");
  $("#" + id).css("display", "inherit");
}
