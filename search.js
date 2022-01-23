function search() {
    var input, filter, ul, li, a, i, txtValue, text;
    input = document.getElementById("myInput1");
    filter = input.value.toUpperCase();
    ul = document.getElementById("myUL");
    li = ul.getElementsByTagName("li");
    text = input.value;
    for (i = 0; i < li.length; i++) {
        a = li[i].getElementsByTagName("a")[0];
        txtValue = a.textContent || a.innerText;
        if (txtValue.toUpperCase().indexOf(filter) > -1) {
            li[i].style.display = "block";
            $("#close-list").addClass("open-icon");
        } else {
            li[i].style.display = "none";
        }
        if (input.value == "") {
            li[i].style.display = "none";
        }
        var result = boldString(txtValue, text);
        a.innerHTML = result;
    }
}

function boldString(str, find) {
    var re = new RegExp(find, 'g');
    console.log(re)
    return str.replace(re, '<b style="color:red; id="text-img">' + find + '</b>');
}

function innerProduct() {
    var string = `
    <li id="in-search" class="label-img"><a id="Change-se" href="#">DNS</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">HTTP</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">TCP/IP</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">SMTP</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">Email</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">Firewall</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">Hub</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">Physical Address</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">Bridge</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">Switch</a>
    <li id="in-search" class="label-img"><a id="value-post" href="#">Router</a>`
    document.getElementById("myUL").innerHTML = string;
}
innerProduct();


// var string = `
//     <li id="in-search" class="label-img"><a id="value-post" href="#">Binary Tree</a>
//         <img src="img/AnhGiayNam/giay1.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">Apriori</a>
//         <img src="img/AnhGiayNam/giay2.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">FP-Tree</a>
//         <img src="img/AnhGiayNam/giay3.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">PageRank</a>
//         <img src="img/AnhGiayNam/giay4.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">Queue</a>
//         <img src="img/AoKhoac/aokhoac1.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">Naive Bayes</a>
//         <img src="img/AoKhoac/aokhoac2.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">Data Structure</a>
//         <img src="img/AoKhoac/aokhoac3.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">Algorithm</a>
//         <img src="img/AoKhoac/aokhoac4.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">K-Means</a>
//         <img src="img/Balo - Tui/balo1.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">Buble Sort</a>
//         <img src="img/Balo - Tui/tui2.jpg" alt=""></li>
//     <li id="in-search" class="label-img"><a id="value-post" href="#">Stack</a>
//         <img src="img/Balo - Tui/tui4.jpg" alt=""></li>`