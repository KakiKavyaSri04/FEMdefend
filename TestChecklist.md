# FEMdefend App Test Checklist

## Setup Tests
- [ ] App installs successfully
- [ ] Firebase connection works
- [ ] All permissions are requested
- [ ] Permissions can be granted/denied

## Authentication Tests
- [ ] New user can register
- [ ] User can login
- [ ] Password reset works
- [ ] Logout works

## Emergency Contacts Tests
- [ ] Can add new contact
- [ ] Can edit existing contact
- [ ] Can delete contact
- [ ] Contacts sync with Firebase
- [ ] Contact list displays correctly

## Location Tests
- [ ] App can get current location
- [ ] Location updates work
- [ ] Location caching works
- [ ] Last known location is saved
- [ ] Location permissions work correctly

## Panic Button Tests
- [ ] Button is easily accessible
- [ ] Button responds to press
- [ ] Location is captured on press
- [ ] SMS messages are sent
- [ ] Notifications are sent
- [ ] Emergency contacts are notified
- [ ] Location link in SMS works

## Notification Tests
- [ ] FCM notifications work
- [ ] Local notifications work
- [ ] Notification permissions work
- [ ] Notification actions work
- [ ] Sound and vibration work

## Error Handling Tests
- [ ] No internet connection
- [ ] Location unavailable
- [ ] Permission denied
- [ ] Invalid phone numbers
- [ ] Firebase errors
- [ ] SMS sending failures

## Performance Tests
- [ ] App launches quickly
- [ ] Smooth navigation
- [ ] No UI freezes
- [ ] Battery usage is reasonable
- [ ] Data usage is reasonable

## Security Tests
- [ ] User data is secure
- [ ] Firebase rules work
- [ ] Permissions are enforced
- [ ] No sensitive data in logs
- [ ] Session handling works

## Notes:
- Test on multiple Android versions
- Test on different screen sizes
- Test with slow internet
- Test with location off
- Test with permissions denied 