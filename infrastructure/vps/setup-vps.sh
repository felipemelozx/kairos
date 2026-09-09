#!/bin/bash

# Initial VPS setup script for Kairos
# Run this once to prepare your VPS for deployment

set -e

# Configuration (update these)
VPS_HOST="your-vps-ip-or-domain"
VPS_USER="root"  # or your sudo user
DOMAIN="api.yourdomain.com"  # Your API domain
EMAIL="your@email.com"  # For Let's Encrypt

echo "🔧 Setting up VPS for Kairos deployment..."

# Install Docker and Docker Compose
ssh ${VPS_USER}@${VPS_HOST} << ENDSSH
  # Update system
  apt-get update && apt-get upgrade -y

  # Install dependencies
  apt-get install -y curl git nginx certbot python3-certbot-nginx

  # Install Docker
  if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    usermod -aG docker \$USER
  fi

  # Install Docker Compose
  if ! command -v docker-compose &> /dev/null; then
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-\$(uname -s)-\$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
  fi

  # Create application directory
  mkdir -p /opt/kairos
  mkdir -p /opt/kairos-backups

  # Setup firewall
  ufw allow 22/tcp
  ufw allow 80/tcp
  ufw allow 443/tcp
  ufw --force enable

  echo "✅ VPS setup complete!"
ENDSSH

echo "📝 Next steps:"
echo "1. Update DNS records to point ${DOMAIN} to ${VPS_HOST}"
echo "2. Run: ssh ${VPS_USER}@${VPS_HOST} 'certbot --nginx -d ${DOMAIN} --email ${EMAIL} --agree-tos'"
echo "3. Copy infrastructure/vps/nginx.conf to /etc/nginx/sites-available/kairos on VPS"
echo "4. Create symlink: ln -s /etc/nginx/sites-available/kairos /etc/nginx/sites-enabled/"
echo "5. Restart nginx: systemctl restart nginx"
echo "6. Deploy backend using: ./infrastructure/vps/deploy-backend.sh"
