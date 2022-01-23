(function() {
    // Functions
    function buildQuiz() {
        // variable to store the HTML output
        const output = [];

        // for each question...
        myQuestions.forEach(
            (currentQuestion, questionNumber) => {

                // variable to store the list of possible answers
                const answers = [];

                // and for each available answer...
                for (letter in currentQuestion.answers) {

                    // ...add an HTML radio button
                    answers.push(
                        `<label>
                        ${letter} :
                ${currentQuestion.answers[letter]}
                <input type="radio" name="question${questionNumber}" value="${letter}">
              </label>`
                    );
                }

                // add this question and its answers to the output
                output.push(
                    `<div class="slide">
              <div class="question"> ${currentQuestion.question} </div>
              <div class="answers"> ${answers.join("")} </div>
            </div>`
                );
            }
        );

        // finally combine our output list into one string of HTML and put it on the page
        quizContainer.innerHTML = output.join('');
    }

    function showResults() {

        // gather answer containers from our quiz
        const answerContainers = quizContainer.querySelectorAll('.answers');

        // keep track of user's answers
        let numCorrect = 0;

        // for each question...
        myQuestions.forEach((currentQuestion, questionNumber) => {

            // find selected answer
            const answerContainer = answerContainers[questionNumber];
            const selector = `input[name=question${questionNumber}]:checked`;
            const userAnswer = (answerContainer.querySelector(selector) || {}).value;

            // if answer is correct
            if (userAnswer === currentQuestion.correctAnswer) {
                // add to the number of correct answers
                numCorrect++;

                // color the answers green
                answerContainers[questionNumber].style.color = 'lightgreen';
            }
            // if answer is wrong or blank
            else {
                // color the answers red
                answerContainers[questionNumber].style.color = 'red';
            }
        });

        // show number of correct answers out of total
        resultsContainer.innerHTML = `${numCorrect}`;
        Coursetro.setInstructor("Trí", numCorrect);
    }

    function showSlide(n) {
        slides[currentSlide].classList.remove('active-slide');
        slides[n].classList.add('active-slide');
        currentSlide = n;
        if (currentSlide === 0) {
            previousButton.style.display = 'none';
        } else {
            previousButton.style.display = 'inline-block';
        }
        if (currentSlide === slides.length - 1) {
            nextButton.style.display = 'none';
            submitButton.style.display = 'inline-block';
        } else {
            nextButton.style.display = 'inline-block';
            submitButton.style.display = 'none';
        }
    }

    function showNextSlide() {
        showSlide(currentSlide + 1);
    }

    function showPreviousSlide() {
        showSlide(currentSlide - 1);
    }

    // Variables
    const quizContainer = document.getElementById('quiz');
    const resultsContainer = document.getElementById('test');
    const submitButton = document.getElementById('submit');
        const myQuestions = [{
            question: "Câu 1. Thiết bị nào hoạt động ở tầng Physical:",
            answers: {
                a: "Repeater",
                b: "Card mạng",
                c: "Hub",
                d: "Cả 3 đáp án trên"
            },
            correctAnswer: "d"
        },
        {
            question: "Câu 2. Để cấp phát động địa chỉ IP, ta có thể sử dụng dịch vụ có giao thức nào?",
            answers: {
                a: "Dùng giao thức DHCP",
                b: "Dùng giao thức FTP",
                c: "Dùng giao thức DNS",
                d: "Dùng giao thức HTTP"
            },
            correctAnswer: "a"
        },
        {
            question: "Câu 3. Địa chỉ IP 192.168.1.1:",
            answers: {
                a: "Thuộc lớp B",
                b: "Thuộc lớp C",
                c: "Là địa chỉ riêng",
                d: "B và C đúng"
            },
            correctAnswer: "d"
        },
        {
            question: "Câu 4. Tầng nào trong mô hình OSI làm việc với các tín hiệu điện:",
            answers: {
                a: "Data Link",
                b: "Network",
                c: "Physical",
                d: "Transport"
            },
            correctAnswer: "c"
        },
        {
            question: "Câu 5. Giao thức nào thuộc tầng Application:",
            answers: {
                a: "IP",
                b: "HTTP",
                c: "NFS",
                d: "TCP"
            },
            correctAnswer: "b"
        },
        {
            question: "Câu 6. Các thành phần tạo nên mạng là:",
            answers: {
                a: "Máy tính, hub, switch",
                b: "Network adapter, cable",
                c: "Protocol",
                d: "Tất cả đều đúng"
            },
            correctAnswer: "d"
        },
        {
            question: "Câu 7. Chức năng chính của router là:",
            answers: {
                a: "Kết nối network với network",
                b: "Chia nhỏ broadcast domain",
                c: "A và B đều đúng",
                d: "A và B đều sai"
            },
            correctAnswer: "c"
        },
        {
            question: "Câu 8. Protocol là:",
            answers: {
                a: "Là các qui tắc để cho phép các máy tính có thể giao tiếp được với nhau",
                b: "Một trong những thành phần không thể thiếu trong hệ thống mạng",
                c: "A và B đúng",
                d: "A và B sai"
            },
            correctAnswer: "a"
        },
        {
            question: "Câu 9. Protocol nào được sử dụng cho mạng Internet:",
            answers: {
                a: "TCP/IP",
                b: "Netbeui",
                c: "IPX/SPX",
                d: "Tất cả ý trên"
            },
            correctAnswer: "a"
        },
        {
            question: "Câu 10. Các chuẩn JPEG, TIFF, ASCII, EBCDIC do tầng nào của mô hình OSI định nghĩa:",
            answers: {
                a: "Transport",
                b: "Network",
                c: "Application",
                d: "Presentation"
            },
            correctAnswer: "c"
        }
    ];

    // Kick things off
    buildQuiz();

    // Pagination
    const previousButton = document.getElementById("previous");
    const nextButton = document.getElementById("next");
    const slides = document.querySelectorAll(".slide");
    let currentSlide = 0;

    // Show the first slide
    showSlide(currentSlide);

    // Event listeners
    submitButton.addEventListener('click', showResults);
    previousButton.addEventListener("click", showPreviousSlide);
    nextButton.addEventListener("click", showNextSlide);
})();