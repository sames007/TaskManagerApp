-- Add ProfilePicturePath column if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS ProfilePicturePath VARCHAR(255);

-- Update any existing users to have a default profile picture path
UPDATE users SET ProfilePicturePath = '/edu/farmingdale/taskmanagerapp/images/profilePicture.png' WHERE ProfilePicturePath IS NULL;

-- Update existing ProfilePicture column to ProfilePicturePath if needed
-- First check if the old column exists
SET @exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_name = 'users'
    AND column_name = 'ProfilePicture'
    AND table_schema = DATABASE()
);

-- If the old column exists, migrate data and drop it
SET @sql := IF(
    @exists > 0,
    'ALTER TABLE users 
     DROP COLUMN ProfilePicture;',
    'SELECT "ProfilePicture column does not exist, no migration needed";'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt; 