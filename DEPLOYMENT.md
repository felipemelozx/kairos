# 🚀 Kairos Deployment Guide

## Quick Reference

### Frontend (Vercel) - Automatic ✨
1. Push to `main` branch
2. GitHub Actions deploys to Vercel
3. Frontend live in ~2 minutes

### Backend (VPS) - Automatic ✨
1. Push to `main` branch
2. GitHub Actions deploys to your VPS
3. Backend live in ~5 minutes

## One-Time Setup

### Step 1: Vercel Setup (5 minutes)

1. **Create Vercel account:**
   - Go to https://vercel.com
   - Sign up with GitHub

2. **Install Vercel CLI:**
```bash
npm install -g vercel
```

3. **Link frontend project:**
```bash
cd apps/frontend
vercel link
```

4. **Get your credentials:**
```bash
# Run this to get your token
vercel login

# Get IDs from vercel.json
cat .vercel/project.json
```

5. **Add GitHub Secrets:**
   - Go to: https://github.com/felipemelozx/kairos/settings/secrets/actions
   - Add: `VERCEL_TOKEN`
   - Add: `VERCEL_ORG_ID`
   - Add: `VERCEL_PROJECT_ID`

### Step 2: VPS Setup (15 minutes)

1. **Prepare your VPS:**
   - Minimum: 2 CPUs, 4GB RAM, 40GB storage
   - OS: Ubuntu 22.04 LTS recommended
   - Ports: 22, 80, 443 must be open

2. **Generate SSH key:**
```bash
ssh-keygen -t rsa -b 4096 -C "kairos-deploy" -f ~/.ssh/kairos_deploy
```

3. **Copy key to VPS:**
```bash
ssh-copy-id -i ~/.ssh/kairos_deploy.pub root@your-vps-ip
```

4. **Configure deployment script:**
```bash
# Edit infrastructure/vps/deploy-backend.sh
nano infrastructure/vps/deploy-backend.sh

# Update these lines:
VPS_HOST="your-vps-ip-or-domain"
VPS_USER="root"
```

5. **Run initial VPS setup:**
```bash
./infrastructure/vps/setup-vps.sh
```

6. **Setup SSL certificate:**
```bash
ssh root@your-vps-ip
certbot --nginx -d api.yourdomain.com --email your@email.com --agree-tos
```

7. **Configure Nginx:**
```bash
# From your local machine
scp infrastructure/vps/nginx.conf root@your-vps-ip:/tmp/nginx.conf

ssh root@your-vps-ip
cp /tmp/nginx.conf /etc/nginx/sites-available/kairos
ln -s /etc/nginx/sites-available/kairos /etc/nginx/sites-enabled/
nginx -t  # Test configuration
systemctl restart nginx
```

8. **Add GitHub Secrets:**
   - Go to: https://github.com/felipemelozx/kairos/settings/secrets/actions
   - Add: `VPS_HOST` (your VPS IP/domain)
   - Add: `VPS_USER` (root or sudo user)
   - Add: `VPS_SSH_PRIVATE_KEY` (contents of ~/.ssh/kairos_deploy)

### Step 3: DNS Configuration (5 minutes)

Point your domains to the correct servers:

| Domain | Points To | Type |
|--------|-----------|------|
| `yourdomain.com` | Vercel | CNAME |
| `www.yourdomain.com` | Vercel | CNAME |
| `api.yourdomain.com` | Your VPS IP | A |

**For Vercel:**
```
Type: CNAME
Name: @ (or www)
Value: cname.vercel-dns.com
```

**For VPS:**
```
Type: A
Name: api
Value: your-vps-ip-address
```

## Test Everything

### 1. Test Backend Deployment
```bash
# Manual deployment test
./infrastructure/vps/deploy-backend.sh

# Check health
curl http://api.yourdomain.com/actuator/health
```

### 2. Test Frontend Deployment
```bash
cd apps/frontend
vercel --prod
```

### 3. Test Full Stack
1. Visit: `https://yourdomain.com`
2. Frontend should load from Vercel
3. API calls should go to `https://api.yourdomain.com`
4. Check browser console for errors

## Continuous Deployment

### Development Workflow
```bash
# Create feature branch
git checkout -b feature/new-feature

# Make changes
git add .
git commit -m "feat: add new feature"

# Push and create PR
git push origin feature/new-feature
```

### Automatic Deployments

| Branch | Event | Action |
|--------|-------|--------|
| `develop` | Pull Request | Run tests (CI) |
| `main` | Merge | Deploy to production |

### Manual Deployment (if needed)

**Frontend:**
```bash
cd apps/frontend
vercel --prod
```

**Backend:**
```bash
./infrastructure/vps/deploy-backend.sh
```

## Monitoring

### Health Checks

**Backend:**
```bash
curl http://api.yourdomain.com/actuator/health
```

**Frontend:**
- Visit Vercel dashboard
- Check deployment logs

### Logs

**Backend (on VPS):**
```bash
ssh root@your-vps-ip
docker logs -f kairos-backend
docker logs -f kairos-postgres
```

**Frontend (Vercel):**
- Visit Vercel dashboard
- View deployment logs

### Database Backups

Backups are created automatically in `/opt/kairos-backups/` on VPS.

To restore:
```bash
ssh root@your-vps-ip
docker exec -i kairos-postgres psql -U kairos kairos_db < /opt/kairos-backups/kairos_db_YYYYMMDD_HHMMSS.sql
```

## Troubleshooting

### Deployment Fails

**Check GitHub Actions logs:**
- Go to: https://github.com/felipemelozx/kairos/actions
- Click on failed workflow
- Review error messages

**Common Issues:**

1. **SSH connection fails**
   - Verify `VPS_SSH_PRIVATE_KEY` is correct
   - Check VPS allows SSH from GitHub Actions IPs
   - Test: `ssh -i ~/.ssh/kairos_deploy root@your-vps-ip`

2. **Vercel deployment fails**
   - Verify `VERCEL_TOKEN` is valid
   - Check `VERCEL_ORG_ID` and `VERCEL_PROJECT_ID`
   - Test: `vercel whoami`

3. **Backend health check fails**
   - Check Docker containers are running: `ssh root@vps "docker ps"`
   - Review logs: `ssh root@vps "docker logs kairos-backend"`
   - Verify database connection

### Rollback

**Frontend:**
```bash
cd apps/frontend
vercel rollback
```

**Backend:**
```bash
ssh root@your-vps-ip
cd /opt/kairos
git log --oneline -10  # Find previous commit
git checkout <previous-commit>
docker-compose up -d --build
```

## Support

If you encounter issues:

1. Check logs first
2. Review this guide
3. Check GitHub Actions logs
4. Consult main documentation: [MONOREPO.md](MONOREPO.md)

---

**Happy deploying! 🚀**
