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

var clouds = {};

function register_cloud_data(cloud_id, cloud_data) {
  clouds[cloud_id] = cloud_data;
}

function rect_contains(r, x, y) {
  return x >= r.x && x <= (r.x + r.w) && y >= r.y && y <= (r.y + r.h);
}

function get_word_id(event) {
  var id = event.data.cloud_id;
  var x = event.pageX - event.currentTarget.offsetLeft;
  var y = event.pageY - event.currentTarget.offsetTop;
  var words = clouds[id];
  for (var i = 0; i < words.length; i++) {
    var w = words[i];
    if (rect_contains(w.r, x, y)) {
      return w.id;
    }
  }
  return null;
}

function cloud_click_handler(event) {
  var word_id = get_word_id(event);
  if (word_id) {
    word(word_id);
  }
}

function cloud_mousemove_handler(event) {
  var img = event.currentTarget;
  var word_id = get_word_id(event);
  if (word_id) {
    img.style.cursor = "pointer";
  } else {
    img.style.cursor = "default";
  }
}

function register_cloud_handler(i, cloud_figure) {
  var id = cloud_figure.getAttribute("data-cloud-id");
  var img = $("img.wordcloud", cloud_figure)[0];
  $(img).on("click", { cloud_id: id }, cloud_click_handler);
  $(img).on("mousemove", { cloud_id: id }, cloud_mousemove_handler);
}

$(function() {
  $.each($("figure.wordcloud"), register_cloud_handler);
});
