function scroll_to_page() {
  window.location.hash = "#main-menu";
}

function video_jump(pos) {
  var player = $("#main_video")[0];
  if (player.paused) {
    player.play();
  }
  player.currentTime = pos;
  scroll_to_page();
}

function innerpage(page_id) {
  $("article.innerpage").css("display", "none");
  $("#" + page_id).css("display", "inherit");
  scroll_to_page();
}

function glossary(part_id) {
  $("div.glossary-part").css("display", "none");
  $("#glossary-part-" + part_id).css("display", "inherit");
}

function word(word_id) {
  $("#word .innerpage").load("words/" + word_id + ".inc.html", function() {
    innerpage("word");
  });
}
