from schedule import Scheduler
import time
import threading
import logging

logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', level=logging.INFO)

def log_info(msg):
    logging.log(logging.INFO, msg)

class CustomScheduler(object):
    """
    Usage: https://github.com/dbader/schedule/wiki/How-to-run-multiple-job-parallel-without-blocking-main-thread%3F
    """

    def __init__(self, schedule):
        self.schedule = schedule

    @property
    def get_job(self):
        "get number of job for given scheduler"
        return self.schedule.jobs

    def run(self):
        while True:
            self.schedule.run_pending()
            time.sleep(1)

    def threaded_schedule(self):
        """this mehtod run a schedule on a threaded system
        Note that only one job can be assign to given scheduler object
        or create new object"""

        assert len(
            self.schedule.jobs) == 1, "there should be one job per Scheduler"
        t1 = threading.Thread(target=self.run)
        t1.daemon = True
        t1.start()

def dummy_task(param):
    log_info(param)

# Para probar el uso del script
if __name__ == "__main__":
    log_info("Empezando tareas")

    #Creación del scheduler
    schedule1 = Scheduler()

    #Define frecuencia y método a ejecutar
    schedule1.every(1).days.do(dummy_task, "Parámetro")
    sched1 = CustomScheduler(schedule1)
    sched1.threaded_schedule()

    log_info("Todas las tareas se empezaron")

    #Simula hilo principal
    while True:
        pass