let myChart;
let currentUnit = 1;

function setUnit(btn, unitValue) {
    document.querySelectorAll('#unit-selector .btn').forEach(b => {
        b.classList.remove('btn-dark', 'active');
        b.classList.add('btn-outline-dark');
    });
    btn.classList.add('btn-dark', 'active');
    btn.classList.remove('btn-outline-dark');

    // 단위 계산
    currentUnit = (unitValue === 0) ? 1 : (unitValue * 60) / 15;

    // 단위 변경 시 차트 초기화
    if (myChart) {
        myChart.destroy();
        myChart = null;
    }
    fetchData();
}

function fetchData() {
    fetch(`/api/history?range=15&unit=${currentUnit}`)
        .then(res => res.json())
        .then(data => {
            updateStatusDot(data.lastSeen);
            renderChart(data.history);
        })
        .catch(err => console.error("데이터 로드 실패:", err));
}

function renderChart(serverData) {
    if (!serverData || serverData.length === 0) return;

    // 서버 데이터를 시간순 정렬 (혹시 모를 꼬임 방지)
    serverData.sort((a, b) => a.time.localeCompare(b.time));

    const labels = serverData.map(d => d.time);
    const cpuData = serverData.map(d => d.avgCpu);
    const memData = serverData.map(d => d.avgMem);

    // Peak CPU 업데이트
    const maxCpu = Math.max(...cpuData);
    if (document.getElementById('peak-cpu')) {
        document.getElementById('peak-cpu').innerText = maxCpu.toFixed(1) + "%";
    }

   if (!myChart) {
           const ctx = document.getElementById('rollingChart').getContext('2d');
           myChart = new Chart(ctx, {
               type: 'line',
               data: {
                   labels: labels,
                   datasets: [
                       {
                           label: 'CPU Usage',
                           data: cpuData,
                           borderColor: '#dc3545',
                           backgroundColor: 'rgba(220, 53, 69, 0.1)',
                           fill: true,
                           tension: 0.2,
                           pointRadius: 4,
                           pointHoverRadius: 6,
                           borderWidth: 2
                       },
                       {
                           label: 'Memory Usage',
                           data: memData,
                           borderColor: '#0d6efd',
                           backgroundColor: 'rgba(13, 110, 253, 0.1)',
                           fill: true,
                           tension: 0.2,
                           pointRadius: 4,
                           pointHoverRadius: 6,
                           borderWidth: 2
                       }
                   ]
               },
               options: {
                   responsive: true,
                   maintainAspectRatio: false,
                   layout: {
                       padding: {
                           top: 20 // ⭐ 여백을 조금 더 넉넉히 줍니다.
                       }
                   },
                   animation: { duration: 0 },
                   interaction: {
                       mode: 'index',
                       intersect: false
                   },
                   scales: {
                       y: {
                           min: 0,
                           max: 110, // ⭐ 핵심: 100이 아닌 110으로 설정해서 천장을 뚫어줍니다.
                           beginAtZero: true,
                           ticks: {
                               stepSize: 20,
                               // ⭐ 숫자가 110까지 나오면 이상하니까 100까지만 표시
                               callback: function(value) {
                                   if (value <= 100) return value + '%';
                                   return '';
                               }
                           }
                       },
                       x: {
                           grid: { display: false },
                           ticks: { maxRotation: 0 }
                       }
                   },
                   plugins: {
                       tooltip: { enabled: true }
                   }
               }
           });
    } else {
        // 기존 차트에 데이터만 덮어쓰기
        myChart.data.labels = labels;
        myChart.data.datasets[0].data = cpuData;
        myChart.data.datasets[1].data = memData;

        // 'none'으로 업데이트하여 즉시 반영 (춤 방지)
        myChart.update('none');
    }
}

function updateStatusDot(lastSeen) {
    const dot = document.getElementById('status-dot');
    if (!dot) return;
    if (lastSeen < 15) dot.style.backgroundColor = '#28a745';
    else if (lastSeen < 60) dot.style.backgroundColor = '#ffc107';
    else dot.style.backgroundColor = '#bbb';
}

// 1초마다 갱신
setInterval(fetchData, 1000);
fetchData();