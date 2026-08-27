UPDATE tourism_contents
SET thumbnail = 'https://' || SUBSTR(thumbnail, 8)
WHERE LOWER(thumbnail) LIKE 'http://%';

UPDATE tourism_content_images
SET original_image_url = 'https://' || SUBSTR(original_image_url, 8)
WHERE LOWER(original_image_url) LIKE 'http://%';
