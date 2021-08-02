FROM python:3.8.11-slim

RUN useradd -ms /bin/bash python

USER python
WORKDIR /home/python

COPY ./requirements.txt ./
RUN pip install -r requirements.txt

COPY --chown=python:python . .

CMD ["python", "pii_data_generator.py"]