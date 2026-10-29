"""
Lambda uruchamiana po wgraniu pliku do bucketa S3 (zdarzenie s3:ObjectCreated).

Minimalne "przetwarzanie" pliku: odczyt metadanych obiektu, zapis do logow CloudWatch
i oznaczenie obiektu tagiem "processed". Przy wiekszych plikach dodatkowo wysyla
powiadomienie SNS. Latwo rozszerzyc np. o generowanie miniaturek (Pillow).
"""
import json
import os
import urllib.parse

import boto3

s3 = boto3.client("s3")
sns = boto3.client("sns")

LARGE_FILE_BYTES = 5 * 1024 * 1024
SNS_TOPIC_ARN = os.environ.get("SNS_TOPIC_ARN")


def handler(event, context):
    results = []
    for record in event.get("Records", []):
        bucket = record["s3"]["bucket"]["name"]
        key = urllib.parse.unquote_plus(record["s3"]["object"]["key"])

        head = s3.head_object(Bucket=bucket, Key=key)
        info = {
            "bucket": bucket,
            "key": key,
            "size": head.get("ContentLength"),
            "contentType": head.get("ContentType"),
        }
        print("Uploaded file:", json.dumps(info))

        s3.put_object_tagging(
            Bucket=bucket,
            Key=key,
            Tagging={"TagSet": [{"Key": "processed", "Value": "true"}]},
        )

        if SNS_TOPIC_ARN and info["size"] and info["size"] > LARGE_FILE_BYTES:
            sns.publish(
                TopicArn=SNS_TOPIC_ARN,
                Subject="Duzy plik w czacie",
                Message=json.dumps(info, indent=2),
            )

        results.append(info)

    return {"processed": len(results), "files": results}
