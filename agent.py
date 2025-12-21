  import psutil
  import requests
  import time

  # 1. 자바 서버 주소
  SERVER_URL = "http://192.168.16.1:8080/api/metrics"

  # 2. 이 컴퓨터의 이름 설정
  AGENT_NAME = "Fedora-Server"

  while True:
      try:
          # CPU, Memory 정보 추출
          cpu_load = psutil.cpu_percent(interval=1)
          mem = psutil.virtual_memory()
          mem_usage = mem.percent

          # 데이터 묶기
          payload = {
              "agent_name": AGENT_NAME,
              "cpu": cpu_load,
              "mem": mem_usage
          }

          # 서버로 전송
          response = requests.post(SERVER_URL, json=payload, timeout=5)

          if response.status_code == 200:
              print(f"✅ [{AGENT_NAME}] 전송 성공: CPU {cpu_load}%, MEM {mem_usage}%")
          else:
              print(f"⚠️ 서버 응답 에러 (상태코드: {response.status_code})")

      except Exception as e:
          print(f"❌ 서버 연결 실패: {e}")


      time.sleep(1)
