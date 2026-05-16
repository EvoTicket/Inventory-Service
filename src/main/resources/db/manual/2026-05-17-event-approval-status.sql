UPDATE inventory_service.events
SET approval_status = 'PENDING'
WHERE approval_status IS NULL;

ALTER TABLE inventory_service.events
ALTER COLUMN approval_status SET DEFAULT 'PENDING';

ALTER TABLE inventory_service.events
ALTER COLUMN approval_status SET NOT NULL;
