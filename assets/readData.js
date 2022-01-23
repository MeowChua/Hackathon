$(document).ready(function() {
    $('#load_data').click(function() {
        $.ajax({
            url: "asb1.csv",
            dataType: "text",
            success: function(data) {
                var employee_data = data.split(/\r?\n|\r/);
                var table_dat a = [];
                var img = [];
                for (var count = 1; count < employee_data.length; count++) {
                    if (2 == employee_data[count].split("Sa Pa,").length) {
                        //console.log(employee_data[count].length, employee_data[count].split("Đà Lạt"));
                        var info = employee_data[count].split("Sa Pa,");
                        table_data.push(info[1]);
                        console.log(info);
                    }
                }
                //$('#employee_table').html(table_data[1]);
                for (var index = 0; index < table_data.length; index++) {
                    var div = document.createElement("div");
                    div.innerHTML = table_data[index];
                    document.body.appendChild(div);
                }
                var img = document.createElement("img");
                img.setAttribute("src", "img/daLat.jpg");
                img.setAttribute("width", "500px");
                document.body.appendChild(img);
            }
        });
    });
});